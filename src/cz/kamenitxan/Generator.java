package cz.kamenitxan;

import com.googlecode.jatl.Html;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.xfer.FileSystemFile;

import javax.json.*;
import javax.json.stream.JsonParsingException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("HardcodedFileSeparator")
public class Generator {
	private final Lists lists = Lists.getInstance();
	private String guildName = "Luzanky";
	private String realm = "Thunderhorn";
	private final int ILVL = 500;

	private Dao<Character, String> dao = null;

	private int tanks = 0;
	private int heals = 0;
	private int dpss = 0;
	private int atanks = 0;
	private int aheals = 0;
	private int adpss = 0;

	private int timeOuts = 0;
	private int updates = 0;

	public Generator() {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		try {
			final String databaseUrl = "jdbc:sqlite:raiders.db";
			final ConnectionSource connectionSource = new JdbcConnectionSource(databaseUrl);
			dao = DaoManager.createDao(connectionSource, Character.class);
			if (!dao.isTableExists()) {
				TableUtils.createTable(connectionSource, Character.class);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public void start(String[] args) {
		if (args.length != 0) {
			guildName = args[0];
			realm = args[1];
		}
		queryGuild();
		getData();
		generateHTML();
	}

	private void queryGuild() {
		System.out.println("Běh zahájen");
		InputStream is = null;
		final String host = "http://eu.battle.net/api/";
		while (is == null) {
			try {
				final URL url = new URL(host + "wow/guild/" + realm + "/" + guildName + "?fields=members");
				is = url.openStream();
			} catch (FileNotFoundException ex) {
				System.out.println(ex.getLocalizedMessage());
				System.out.println(ex.getMessage());
				final String error = "Postava  na serveru nenalezena";
				System.out.println(error);
			} catch (IOException ex) {
				System.out.println(ex.getLocalizedMessage());
			}
		}
		JsonReader jsonReader;
		JsonObject jsonObject;
		JsonArray members;
		try{
			jsonReader = Json.createReader(is);
			jsonObject = jsonReader.readObject();
			members = jsonObject.getJsonArray("members");
			members.forEach(m -> addChar(m));
		} catch (JsonParsingException ex) {
			System.out.println(ex.getMessage());
			final BufferedReader in = new BufferedReader(new InputStreamReader(is));
			String inputLine;
			try {
				while ((inputLine = in.readLine()) != null) {
					System.out.println(inputLine);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void addChar(JsonValue ch) {
		JsonObject jsonCharacter = (JsonObject) ch;
		final int rank = jsonCharacter.getInt("rank");
		jsonCharacter = jsonCharacter.getJsonObject("character");
		if (jsonCharacter.getInt("level") == 100) {
			try {
				if (!dao.idExists(jsonCharacter.getString("name"))) {
					final Character character = new Character(realm, jsonCharacter.getString("name"), rank);
					dao.createIfNotExists(character);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}

			//characters.add(new Character(realm, jsonCharacter.getString("name"), rank));
		}
	}

	private void getData() {
		final ExecutorService executor = Executors.newFixedThreadPool(16);

		for (Character character : dao) {
			executor.submit(() -> queryAPI(character));
		}
		executor.shutdown();

		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//for (Character character : dao) {
		//	queryAPI(character);
		//}
	}

	private void queryAPI(Character character) {
		InputStream is = null;
		while (is == null) {
			try{
				final String host = "http://eu.battle.net/api/";
				final URL url = new URL(host + "wow/character/" + character.getRealm() + "/" +  character.getName());
				// System.out.println(url.toString());
				final HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setReadTimeout(1500); //1,5 vteřiny

				is = con.getInputStream();
			} catch (FileNotFoundException ex) {
				final String error = "Postava " + character.getName() + " na serveru " + character.getRealm() + " nenalezena";
				System.out.println(error);
				try {
					dao.delete(character);
				} catch (SQLException e) {
					e.printStackTrace();
				}
				return;
			} catch (IOException ex) {
				final String error = ex.getLocalizedMessage();
				System.out.println("IOEX "+ character.getName() + ": " + error);
				if (error.contains("Read timed out")) {
					timeOuts++;
				}
				Thread.yield();
			}
		}
		JsonReader jsonReader = Json.createReader(is);
		JsonObject jsonObject = jsonReader.readObject();
		final int lastModified = jsonObject.getInt("lastModified");

		if (lastModified != character.getLastModified()) {
			System.out.println(character.getName() + " aktualizovnán");
			is = null;
			System.out.println(character.getLastModified() + " " + lastModified);
			character.setLastModified(lastModified);
			updates++;
			while (is == null) {
				try {
					final String host = "http://eu.battle.net/api/";
					final URL url = new URL(host + "wow/character/" + character.getRealm() + "/" + character.getName() +
							"?fields=guild,items,titles,talents,professions,achievements");
					// System.out.println(url.toString());
					final HttpURLConnection con = (HttpURLConnection) url.openConnection();
					con.setReadTimeout(1500); //1,5 vteřiny

					is = con.getInputStream();
				} catch (FileNotFoundException ex) {
					final String error = "Postava " + character.getName() + " na serveru " + character.getRealm() + " nenalezena";
					System.out.println(error);
					return;
				} catch (IOException ex) {
					final String error = ex.getLocalizedMessage();
					System.out.println("IOEX " + character.getName() + ": " + error);
					if (error.contains("Read timed out")) {
						timeOuts++;
					}
					Thread.yield();
				}
			}

			// FIX: ošetření, když nejde internet
			jsonReader = Json.createReader(is);
			jsonObject = jsonReader.readObject();

			// System.out.println(jsonObject.toString());

			final JsonObject guild = jsonObject.getJsonObject("guild");
			final JsonObject items = jsonObject.getJsonObject("items");
			final JsonArray titles = jsonObject.getJsonArray("titles");
			final JsonArray talents = jsonObject.getJsonArray("talents");
			final JsonArray achievements = jsonObject.getJsonObject("achievements").getJsonArray("achievementsCompleted");

			JsonObject spec = talents.getJsonObject(0);
			if (spec.size() == 7) {
				final String specs = spec.getJsonObject("spec").getString("name", null);
				character.setSpec(specs, true);
				character.setIlvl(items.getInt("averageItemLevelEquipped"));
			} else if (spec.size() == 6) {
				final String specs = spec.getJsonObject("spec").getString("name", null);
				character.setSpec(specs, false);
			}
			spec = talents.getJsonObject(1);
			if (spec.size() == 7) {
				final String specs = spec.getJsonObject("spec").getString("name", null);
				character.setAltSpec(specs, true);
				character.setAltIlvl(items.getInt("averageItemLevelEquipped"));
			} else if (spec.size() == 6) {
				final String specs = spec.getJsonObject("spec").getString("name", null);
				character.setAltSpec(specs, false);
			}

			final JsonObject professions = jsonObject.getJsonObject("professions");
			final JsonObject primary_prof = professions.getJsonArray("primary").getJsonObject(0);
			final JsonObject secondary_prof = professions.getJsonArray("primary").getJsonObject(1);


			jsonReader.close();

			character.setLvl(jsonObject.getInt("level"));
			character.setPlayerClass(jsonObject.getInt("class"));
			character.setRace(jsonObject.getInt("race"));
			character.setPlayerClass(jsonObject.getInt("class"));
			character.setGender(jsonObject.getInt("gender"));
			character.setAchievementPoints(jsonObject.getInt("achievementPoints"));
			character.setAvatar(jsonObject.getString("thumbnail"));
			character.setGuild(guild.getString("name"));
			for (JsonValue i : titles) {
				final JsonObject title = (JsonObject) i;
				if (title.size() == 3) {
					character.setTitle(title.getString("name"));
				}
			}
			character.setPrimaryProf(primary_prof.getString("name"));
			character.setPrimaryProfLvl(primary_prof.getInt("rank"));
			character.setSecondaryProf(secondary_prof.getString("name"));
			character.setSecondaryProfLvl(secondary_prof.getInt("rank"));

			character.setTankChallenge(0);
			character.setHealChallenge(0);
			character.setDpsChallenge(0);
			achievements.stream().filter(a -> Integer.parseInt(a.toString()) > 9570 && Integer.parseInt(a.toString()) < 9590).forEach(a -> {
				switch (Integer.parseInt(a.toString())) {
					case 9578: {
						character.setTankChallenge(1);
						break;
					}
					case 9579: {
						character.setTankChallenge(2);
						break;
					}
					case 9580: {
						character.setTankChallenge(3);
						break;
					}
					case 9584: {
						character.setHealChallenge(1);
						break;
					}
					case 9585: {
						character.setHealChallenge(2);
						break;
					}
					case 9586: {
						character.setHealChallenge(3);
						break;
					}
					case 9572: {
						character.setDpsChallenge(1);
						break;
					}
					case 9573: {
						character.setDpsChallenge(2);
						break;
					}
					case 9574: {
						character.setDpsChallenge(3);
						break;
					}
				}
			});

			processAudit(items, character);
			//System.out.println(jsonObject.toString());

			if (character.getTitle() == null) {
				character.setTitle("");
			}

			try {
				dao.createOrUpdate(character);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private void processAudit(JsonObject items, Character ch) {
		final int lowerILV = 630;
		final int higherIVL = 650;
		ch.setMissingGems(false);

		final JsonObject head = items.getJsonObject("head");
		if (head.getInt("itemLevel") >= lowerILV) {
			checkGem(ch, head);
		}
		final JsonObject neck = items.getJsonObject("neck");
		if (neck.getInt("itemLevel") >= lowerILV) {
			if (neck.getJsonObject("tooltipParams").getInt("enchant", 0) == 0) {ch.setNeckEnch(false);}
			else {ch.setNeckEnch(true);}
			checkGem(ch, neck);
		}
		final JsonObject shoulder = items.getJsonObject("shoulder");
		if (shoulder.getInt("itemLevel") >= lowerILV) {
			checkGem(ch, shoulder);
		}
		final JsonObject back = items.getJsonObject("back");
		if (back.getInt("itemLevel") >= lowerILV) {
			if (back.getJsonObject("tooltipParams").getInt("enchant", 0) == 0) {ch.setBackEnch(false);}
			else {ch.setBackEnch(true);}
			checkGem(ch, back);
		}
		final JsonObject chest = items.getJsonObject("chest");
		if (chest.getInt("itemLevel") >= lowerILV) {
			checkGem(ch, chest);
		}
		final JsonObject wrist = items.getJsonObject("wrist");
		if (wrist.getInt("itemLevel") >= lowerILV) {
			checkGem(ch, wrist);
		}
		final JsonObject hands = items.getJsonObject("hands");
		if (hands.getInt("itemLevel") >= lowerILV) {
			checkGem(ch, hands);
		}
		final JsonObject waist = items.getJsonObject("waist");
		if (waist.getInt("itemLevel") >= lowerILV) {
			checkGem(ch, waist);
		}
		final JsonObject legs = items.getJsonObject("legs");
		if (legs.getInt("itemLevel") >= lowerILV) {
			checkGem(ch, legs);
		}
		final JsonObject feet = items.getJsonObject("feet");
		if (feet.getInt("itemLevel") >= lowerILV) {
			checkGem(ch, feet);
		}
		final JsonObject ring1 = items.getJsonObject("finger1");
		if (ring1.getInt("itemLevel") >= lowerILV) {
			if (ring1.getJsonObject("tooltipParams").getInt("enchant", 0) == 0) {ch.setRing1Ench(false);}
			else {ch.setRing1Ench(true);}
			checkGem(ch, ring1);
		}
		final JsonObject ring2 = items.getJsonObject("finger2");
		if (ring2.getInt("itemLevel") >= lowerILV) {
			if (ring2.getJsonObject("tooltipParams").getInt("enchant", 0) == 0) {ch.setRing2Ench(false);}
			else {ch.setRing2Ench(true);}
			checkGem(ch, ring2);
		}
		final JsonObject trinket1 = items.getJsonObject("trinket1");
		if (trinket1.getInt("itemLevel") >= lowerILV) {
			checkGem(ch, trinket1);
		}
		final JsonObject trinket2 = items.getJsonObject("trinket2");
		if (trinket2.getInt("itemLevel") >= lowerILV) {
			checkGem(ch, trinket2);
		}
		final JsonObject weapon = items.getJsonObject("mainHand");
		if (weapon.getInt("itemLevel") >= higherIVL) {
			if (weapon.getJsonObject("tooltipParams").getInt("enchant", 0) == 0) {ch.setWeaponEnch(false);}
			else {ch.setWeaponEnch(true);}
			checkGem(ch, weapon);
		}
		try {
			dao.createOrUpdate(ch);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	private void checkGem(Character ch, JsonObject item) {
		if (item.getJsonArray("bonusLists").size() > 1) {
			final String bL = item.getJsonArray("bonusLists").toString();
			if (bL.contains(String.valueOf(523)) && bL.contains(String.valueOf(524))) {
				if (item.getJsonObject("tooltipParams").getInt("gem0", 0) == 0) {
					ch.setMissingGems(true);
				}
			}
		}
	}

	private String getAudit(Character ch) {
		String result = "";
		int count = 0;
		if (!ch.isBackEnch()) {
			result += "Back enchant, ";
			count++;
		}
		if (!ch.isNeckEnch()) {
			result += "Neck enchant, ";
			count++;
		}
		if (!ch.isWeaponEnch()) {
			result += "Weapon enchant, ";
			count++;
		}
		if (!ch.isRing1Ench()) {
			result += "Ring1 enchant, ";
			count++;
		}
		if (!ch.isRing2Ench()) {
			result += "Ring2 enchant, ";
			count++;
		}
		if (ch.getMissingGems() > 0) {
			result += "Chybí " + ch.getMissingGems() + " gem(y), ";
			count += ch.getMissingGems();
		}
		if (!result.equals("")) {
			return "<img src=\"img/error.png\" title=\"Chybí: " + result + "\"> " + count;
		}
		return result;
	}


	private void generateHTML() {
		//characters.parallelStream().filter(ch -> ch.getIlvl() > ILVL).forEach(ch -> countRole(ch));
		for (Character ch : dao) {
			if (ch.getIlvl() > ILVL || ch.getAltIlvl() > ILVL) {
				countRole(ch);
			}
		}

		final StringWriter sw = new StringWriter();
		final Html html = new Html(sw);

		html.html();
		html.head();
			html.meta().charset("UTF-8");
			html.title().text("Raiders of Luzanky").end();
			html.link().rel("stylesheet").href("https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css");
			html.link().rel("stylesheet").href("img/tablesorter-2.18.3/css/theme.default.css");
		html.link().rel("stylesheet").href("img/tablesorter-2.18.3/css/theme.dark.css");
			html.raw("<script src=\"http://ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js\"></script>");
			html.raw("<script src=\"img/tablesorter-2.18.3/js/jquery.tablesorter.min.js\"></script>");
			html.raw("<script src=\"img/tablesorter-2.18.3/js/jquery.tablesorter.widgets.js\"></script>");
			html.style().raw(".table-striped>tbody>tr:nth-child(odd) {background-color: rgb(28, 28, 28) !important;}" +
							 ".table {width: auto;} .role {display: none;} td a {color: inherit}").end();
		html.body().style("color: white; background-color: black;");
			html.h1().text("Seznam raiderů Lužánek").end();
			html.p().a().href("img/changelog.html").text("Changelog - seznam změn").endAll();
			html.raw("<table id=\"myTable\" class=\"table table-striped table-condensed tablesorter\" data-sortlist=\"[[3,1]]\"><thead><tr>" +
					"<th>Jméno</th>" +
					"<th>Povolání</th>" +
					"<th data-placeholder=\"treba heal\">Spec</th>" +
					"<th data-value=\">615\">iLVL</th>" +
					"<th>Off-Spec</th>" +
					"<th>Off-Spec iLVL</th>" +
					"<th>Rank</th>" +
					"<th>Audit</th></tr></thead><tbody>");

			//characters.parallelStream().filter(ch -> ch.getIlvl() > ILVL).forEach(ch -> html.raw(createRow(ch)));
			for (Character ch : dao) {
				if (ch.getIlvl() > ILVL || ch.getAltIlvl() > ILVL) {
					html.raw(createRow(ch));
				}
			}

			html.raw("</tbody></table>");
		try {
			html.p().text("Tanků: " + tanks + " (" + (tanks + atanks) + ")").br()
					.text("Healů: " + heals + " (" + (heals + aheals) + ")").br()
					.text("DPS: "   + dpss + " (" +  (dpss  + adpss)  + ")").br()
					.text("Celkem: " + dao.countOf());
		} catch (SQLException e) {
			e.printStackTrace();
		}

		html.p().text(getTime()).end();
		html.p().text("Timeouts: " + timeOuts + " Postav aktualizováno: " + updates).end();
		html.script().raw("$(function(){" +
				"		$(\"#myTable\").tablesorter(" +
									"{theme: 'dark', widgets: [\"zebra\", \"filter\"],}" +
									");" +
					 		  "});").end();
			html.endAll();

		final String result = sw.getBuffer().toString();
		try {
			Files.write(Paths.get("raiders.html"), result.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("HTML vygenerováno");

		try {
			sshUpload("raiders.html");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String createRow(Character ch){
		String spec = "<img src=\"img/ch";
		String altSpec = spec;
		String specName, altSpecName = "";
		int iLvl = ch.getIlvl();
		int altiLvl = ch.getAltIlvl();
		if (ch.getSpecChallenge(1) != 0) {
			spec += ch.getSpecChallenge(1) + ".png\">";
		} else {spec = "";}
		if (ch.getSpecChallenge(2) != 0) {
			altSpec += ch.getSpecChallenge(2) + ".png\">";
		} else {altSpec = "";}
		if (ch.getSpec().isActive()) {
			specName = ch.getSpec().getSpecName();
			if (ch.getAltSpec() != null) {
				altSpecName = "<i>" + ch.getAltSpec().getSpecName() + "</i>";
			}
		} else {
			specName = "<i>" + ch.getSpec().getSpecName() + "</i>";
			altSpecName = ch.getAltSpec().getSpecName();
		}
		String roles;
		if (ch.getAltSpec() != null) {
			roles = lists.getRoleType(ch.getAltSpec().getSpecName()) + lists.getRoleType(ch.getSpec().getSpecName());
		}else {
			roles = lists.getRoleType(ch.getSpec().getSpecName());
		}
		spec = "<img src=\"img/" + Lists.getRole(ch.getSpec().getSpecName()) + ".png\">" + spec
				+ "<span class=\"role\">" + roles + "</span> "
				+ specName;
		if (ch.getAltSpec() != null) {
			altSpec = "<img src=\"img/" + Lists.getRole(ch.getAltSpec().getSpecName()) + ".png\">" + altSpec
					+ "<span class=\"role\">" + roles + "</span> "
					+ altSpecName;
		}else {
			altSpec = "";
		}
		if (altiLvl > iLvl) {
			final String t = spec;
			spec = altSpec;
			altSpec = t;
		}
		if (altiLvl > iLvl) {
			final int t = iLvl;
			iLvl = altiLvl;
			altiLvl = t;
		}
		return ("<tr style=\"color: " + lists.getPClassColor(ch.getPlayerClass()) + "\" ><td>"
				+ "<a href=\"http://eu.battle.net/wow/en/character/" + ch.getRealm() + "/" + ch.getName() +"/advanced\">" + ch.getName() + "</a></td>"
				+ "<td>" + lists.getPClass(ch.getPlayerClass()) + "</td>"
				+ "<td>" + spec + "</td>"
				+ "<td>" + iLvl + "</td>"
				+ "<td>" + altSpec + "</td>"
				+ "<td>" + altiLvl + "</td>"
				+ "<td>" + lists.getRank(ch.getRank()) + "</td>")
				+ "<td>" + getAudit(ch) + "</td>"
				+ "</tr>\n";
	}

	private String getTime(){
		final DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		final Date today = Calendar.getInstance().getTime();
		final String reportDate = df.format(today);
		final double cas = (System.nanoTime() - Main.startTime) / 1000000000;

		return "Generováno " + reportDate + ". Export trval " + cas + " s";
	}

	private void countRole(Character ch) {
		if (ch.getSpec() != null){
			if (Lists.getRole(ch.getSpec().getSpecName()) == 3) {
				dpss += 1;
			} else if (Lists.getRole(ch.getSpec().getSpecName()) == 2) {
				heals += 1;
			} else if (Lists.getRole(ch.getSpec().getSpecName()) == 1) {
				tanks += 1;
			}
		}
		if (ch.getAltSpec() != null) {
			if (Lists.getRole(ch.getAltSpec().getSpecName()) == 3 && Lists.getRole(ch.getSpec().getSpecName()) != Lists.getRole(ch.getAltSpec().getSpecName())) {
				adpss += 1;
			}
			if (Lists.getRole(ch.getAltSpec().getSpecName()) == 2 && Lists.getRole(ch.getSpec().getSpecName()) != Lists.getRole(ch.getAltSpec().getSpecName())) {
				aheals += 1;
			}
			if (Lists.getRole(ch.getAltSpec().getSpecName()) == 1 && Lists.getRole(ch.getSpec().getSpecName()) != Lists.getRole(ch.getAltSpec().getSpecName())) {
				atanks += 1;
			}
		}
	}

	private void sshUpload(String fileName) throws IOException{
		final SSHClient ssh = new SSHClient();
		ssh.loadKnownHosts();
		ssh.addHostKeyVerifier("85:c8:d7:b4:33:1b:28:33:90:78:47:65:96:0b:85:9d");
		ssh.connect("192.168.1.1");
		String pass = "";
		try (BufferedReader br = new BufferedReader(new FileReader("ssh_heslo")))
		{
			pass = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {

			ssh.authPassword("root", pass);
			try (SFTPClient sftp = ssh.newSFTPClient()) {
				sftp.put(new FileSystemFile(fileName), "/tmp/blog/share/");
			}
		} finally {
			ssh.disconnect();
		}
	}
}
