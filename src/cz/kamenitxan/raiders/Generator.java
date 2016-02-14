package cz.kamenitxan.raiders;

import com.googlecode.jatl.Html;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import cz.kamenitxan.raiders.dataHolders.Character;
import cz.kamenitxan.raiders.dataHolders.Lists;
import cz.kamenitxan.raiders.dataHolders.Raid;
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
import java.util.zip.GZIPInputStream;

@SuppressWarnings("HardcodedFileSeparator")
public class Generator {
	private final Lists lists = Lists.getInstance();
	public static String guildName = "Luzanky";
	public static String realm = "Thunderhorn";
	private final int ILVL = 500;
	private boolean forceUpdate = false;

	private Dao<cz.kamenitxan.raiders.dataHolders.Character, String> dao = null;

	private int tanks = 0;
	private int heals = 0;
	private int dpss = 0;
	private int atanks = 0;
	private int aheals = 0;
	private int adpss = 0;

	private int timeOuts = 0;
	private int updates = 0;

    /**
     * Constructor creates database if it's not existing
     */
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

    /**
     * Start generation of rooster
     * @param args guild name and realm
     */
	public void start(String[] args) {
		if (args.length != 0) {
			guildName = args[0];
			realm = args[1];
			if (args[2].equals("force")) {
				forceUpdate = true;
			}
		}
		queryGuild();
		getData();
		generateHTML();
	}

    /**
     * Gets all lvl 100 members from API and saves them to DB.
     */
	private void queryGuild() {
		// BUG: nějak implementovat merged realms. Smrtacka je z wildhameru
		System.out.println("Běh zahájen");
		InputStream is = null;
		final String host = "https://eu.api.battle.net/";
		while (is == null) {
			try {
				final URL url = new URL(host + "wow/guild/" + realm + "/" + guildName + "?fields=members&locale=en_GB&apikey=" + Main.apiKey);
				final HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestProperty("Accept-Encoding", "gzip, deflate");
				con.setReadTimeout(1500); //1,5 vteřiny

				is = new GZIPInputStream(con.getInputStream());
			} catch (FileNotFoundException ex) {
				System.out.println(ex.getLocalizedMessage());
				System.out.println(ex.getMessage());
				final String error = "Postava  na serveru nenalezena";
				System.out.println(error);
			} catch (IOException ex) {
				System.out.println(ex.getLocalizedMessage());
				System.exit(1);
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

    /**
     * @param ch character saved to DB
     */
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

    /**
     * Manages downloading of characters in threads.
     */
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
//		for (Character character : dao) {
//			queryAPI(character);
//		}
	}

    /**
     * @param character to be downloaded from API
     */
	private void queryAPI(Character character) {
		InputStream is = null;
		while (is == null) {
			try{
				// TODO: new api
				final String host = "https://eu.api.battle.net/";
				final URL url = new URL(host + "wow/character/" + character.getRealm() + "/" +  character.getName() + "?locale=en_GB&apikey=" + Main.apiKey);
				// System.out.println(url.toString());
				final HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestProperty("Accept-Encoding", "gzip, deflate");
				con.setReadTimeout(1500); //1,5 vteřiny

				is = new GZIPInputStream(con.getInputStream());
			} catch (FileNotFoundException ex) {
				final String error = "Postava " + character.getName() + ", " + character.getRealm() + " nenalezena";
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
		final long lastModified = jsonObject.getJsonNumber("lastModified").longValue();
		if (lastModified != character.getLastModified() || forceUpdate) {
			//System.out.println(character.getName() + " aktualizovnán");
			is = null;
			//System.out.println(character.getLastModified() + " teď: " + lastModified);
			character.setLastModified(lastModified);
			updates++;
			while (is == null) {
				try {
					// TODO: new api
					final String host = "https://eu.api.battle.net/";
					final URL url = new URL(host + "wow/character/" + character.getRealm() + "/" + character.getName() +
							"?fields=guild,items,titles,talents,professions,achievements,progression&locale=en_GB&apikey=" + Main.apiKey);
					// System.out.println(url.toString());
					final HttpURLConnection con = (HttpURLConnection) url.openConnection();
					con.setRequestProperty("Accept-Encoding", "gzip, deflate");
					con.setReadTimeout(1500); //1,5 vteřiny

					is = new GZIPInputStream(con.getInputStream());
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

			final JsonArray professions = jsonObject.getJsonObject("professions").getJsonArray("primary");
			JsonObject primary_prof = null;
			JsonObject secondary_prof = null;
			if (professions.size() > 0) {
				primary_prof = professions.getJsonObject(0);
				secondary_prof = professions.getJsonObject(1);
			}



			jsonReader.close();

			character.setLvl(jsonObject.getInt("level"));
			character.setPlayerClass(jsonObject.getInt("class"));
			character.setRace(jsonObject.getInt("race"));
			character.setPlayerClass(jsonObject.getInt("class"));
			character.setGender(jsonObject.getInt("gender"));
			character.setAchievementPoints(jsonObject.getInt("achievementPoints"));
			character.setAvatar(jsonObject.getString("thumbnail"));
			character.setGuild(guild.getString("name"));
			if (primary_prof != null) {
				character.setPrimaryProf(primary_prof.getString("name"));
				character.setPrimaryProfLvl(primary_prof.getInt("rank"));
			}
			if (secondary_prof != null) {
				character.setSecondaryProf(secondary_prof.getString("name"));
				character.setSecondaryProfLvl(secondary_prof.getInt("rank"));
			}

			character.setTankChallenge(0);
			character.setHealChallenge(0);
			character.setDpsChallenge(0);
			achievements.stream().filter(a -> Integer.parseInt(a.toString()) > 9570
                                           && Integer.parseInt(a.toString()) < 9590).forEach(a -> {
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
					case 9581: {
						character.setTankChallenge(4);
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
					case 9587: {
						character.setHealChallenge(4);
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
					case 9575: {
						character.setDpsChallenge(4);
					}
				}
			});
			JsonArray raids = jsonObject.getJsonObject("progression").getJsonArray("raids");
			System.out.println(character.getName());
			setRaidProgress(character, raids);

			Audit.processAudit(items, character);
			//System.out.println(jsonObject.toString());

			if (character.getTitle() == null) {
				character.setTitle("");
			}
			try {
				if (!character.getGuild().equals(guildName)) {
					dao.deleteById(character.getName());
					System.out.println("Smazána postava " + character.getName());
				} else {
					dao.update(character);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Gets raid progresion info
	 */
	private void setRaidProgress(Character ch, JsonArray raids) {
		ch.nullRaidProgress();
		for (int i = 32; i <= 34; i++) {
			JsonObject raid = raids.getJsonObject(i);
			JsonArray bosses = raid.getJsonArray("bosses");
			int lfr = 0;
			int normal = 0;
			int heroic = 0;
			int mythic = 0;
			for (JsonValue boss : bosses) {
				if (((JsonObject) boss).getInt("lfrKills") > 0) {
					lfr++;
				}
				if (((JsonObject) boss).getInt("normalKills") > 0) {
					normal++;
				}
				if (((JsonObject) boss).getInt("heroicKills") > 0) {
					heroic++;
				}
				if (((JsonObject) boss).getInt("mythicKills") > 0) {
					mythic++;
				}
			}
			ch.setRaidProgress(i, lfr, normal, heroic, mythic);
		}
	}

    /**
     * Handles generating HTML output
     */
	private void generateHTML() {
		//characters.parallelStream().filter(ch -> ch.getIlvl() > ILVL).forEach(ch -> countRole(ch));
        // ORMLite does not support streams
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
			//html.link().rel("stylesheet").href("img/tablesorter-2.18.3/css/theme.default.css");
			html.link().rel("stylesheet").href("img/tablesorter-2.18.3/css/theme.dark.css");
			html.raw("<script src=\"http://ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js\"></script>");
			html.raw("<script src=\"img/tablesorter-2.18.3/js/jquery.tablesorter.min.js\"></script>");
			html.raw("<script src=\"img/tablesorter-2.18.3/js/jquery.tablesorter.widgets.js\"></script>");
			html.style().raw(".table-striped>tbody>tr:nth-child(odd) {background-color: rgb(28, 28, 28) !important;}" +
							 ".table {width: auto;} .role {display: none;} td a {color: inherit}" +
							 ".ano {width: 10px; height: 10px; background-color: green; display: inline-block;} " +
					 	 	 ".ne {width: 10px; height: 10px; background-color: red; display: inline-block;}").end();
		html.body().style("color: white; background-color: black;");
			html.h1().text("Seznam raiderů Lužánek").end();
			html.p().a().href("img/changelog.html").text("Changelog - seznam změn").endAll();
			html.raw("<table id=\"myTable\" class=\"table table-striped table-condensed tablesorter\"" +
                                                        " data-sortlist=\"[[3,1]]\"><thead><tr>" +
					"<th>Jméno</th>" +
					"<th>Povolání</th>" +
					"<th data-placeholder=\"treba heal\">Spec</th>" +
					"<th data-value=\">630\">iLVL</th>" +
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
							"$('.tablesorter-childRow td').hide();" +
							"$(\"#myTable\").tablesorter(" +
								"{theme: 'dark', widgets: [\"zebra\", \"filter\"], cssChildRow: \"tablesorter-childRow\",}" +
							");\n" +
							"$('.tablesorter').delegate('.toggle', 'click' ,function(){$(this).closest('tr').nextUntil('tr.tablesorter-hasChildRow').find('td').toggle(); return false;});\n" +
				          "});").end();
		html.endAll();

		final String result = sw.getBuffer().toString();
		try {
			Files.write(Paths.get("raiders.html"), result.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("HTML vygenerováno");

		if (Main.sshUpload) {
			try {
				sshUpload("raiders.html");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

    /**
     * @param ch character
     * @return table row with character
     */
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

		// childrow - šířka 8(2,2,2)
		Raid hmR = ch.getRaidProgress().getRaid(32);
		Raid brR = ch.getRaidProgress().getRaid(33);
		Raid hfc = ch.getRaidProgress().getRaid(34);

		String attendance = "";
		for (Boolean attended : ch.getAttendanceHistory()) {
			if (attended) {
				attendance += "<div class=\"ano\"></div>";
			} else {
				attendance += "<div class=\"ne\"></div>";
			}
		}

		String childRow = "<tr class=\"tablesorter-childRow\"><td colspan=\"2\">";
		SimpleDateFormat df = new SimpleDateFormat("hh:mm dd.MM.yy");
		childRow += "Poslední aktualizace: " + df.format(new Date(ch.getLastModified()));
		childRow += "</td>";

		childRow += "<td colspan=\"2\">Progres BRF: " + brR.lfrKills + ","
				+ brR.normalKills + ","
				+ brR.heroicKills + ","
				+ brR.mythicKills + "/7"
				+ "</td>";
		if (hfc != null) {
			childRow += "<td colspan=\"2\">Progres HFC: " + hfc.lfrKills + ","
					+ hfc.normalKills + ","
					+ hfc.heroicKills + ","
					+ hfc.mythicKills + "/13"
					+ "</td>";
		}

		childRow += "<td colspan=\"2\">" + attendance + "</td>";


		return ("<tr style=\"color: " + lists.getPClassColor(ch.getPlayerClass()) + "\"><td>"
				+ "<a href=\"http://eu.battle.net/wow/en/character/" + ch.getRealm() + "/" + ch.getName()
                    +"/advanced\" target=\"_blank\">" + Lists.altsMain(ch.getName()) + "</a></td>"
				+ "<td>" + lists.getPClass(ch.getPlayerClass()) + "</a></td>"
				+ "<td>" + spec + "</td>"
				+ "<td>" + iLvl + "</td>"
				+ "<td>" + altSpec + "</td>"
				+ "<td>" + altiLvl + "</td>"
				+ "<td>" + lists.getRank(ch.getRank()) + "</td>")
				+ "<td>" + "<a href=\"#\"class=\"toggle\"><img src=\"img/info.png\"></a>" + Audit.getAudit(ch)
				+ "<span class=\"role\">" + ch.getActiveStatus() + "</span></td>"
				+ "</tr>\n" +
				childRow;
	}

    /**
     * @return elapsed time of generation
     */
	private String getTime(){
		final DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		final Date today = Calendar.getInstance().getTime();
		final String reportDate = df.format(today);
		final double cas = (System.nanoTime() - Main.startTime) / 1000000000;

		return "Generováno " + reportDate + ". Export trval " + cas + " s";
	}

    /**
     * Adds role to role counter for character
     * @param ch character
     */
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
			if (Lists.getRole(ch.getAltSpec().getSpecName()) == 3
                && Lists.getRole(ch.getSpec().getSpecName()) != Lists.getRole(ch.getAltSpec().getSpecName())) {
				adpss += 1;
			}
			if (Lists.getRole(ch.getAltSpec().getSpecName()) == 2
                && Lists.getRole(ch.getSpec().getSpecName()) != Lists.getRole(ch.getAltSpec().getSpecName())) {
				aheals += 1;
			}
			if (Lists.getRole(ch.getAltSpec().getSpecName()) == 1
                && Lists.getRole(ch.getSpec().getSpecName()) != Lists.getRole(ch.getAltSpec().getSpecName())) {
				atanks += 1;
			}
		}
	}

    /**
     * Uploads result to server with SSH
     * @param fileName name of uploaded file
     * @throws IOException
     */
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
