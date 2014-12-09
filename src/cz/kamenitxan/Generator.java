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
import java.util.ArrayList;
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

	private final String databaseUrl = "jdbc:sqlite:raiders.db";
	private ConnectionSource connectionSource;
	private Dao<Character, ?> dao;

	private int tanks = 0;
	private int heals = 0;
	private int dpss = 0;
	private int atanks = 0;
	private int aheals = 0;
	private int adpss = 0;

	private int timeOuts = 0;

	public Generator() {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		try {
			connectionSource = new JdbcConnectionSource(databaseUrl);
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
		while (is == null) {
			try {
				String host = "http://eu.battle.net/api/";
				URL url = new URL(host + "wow/guild/" + realm + "/" + guildName +
						"?fields=members");
				is = url.openStream();
			} catch (FileNotFoundException ex) {
				System.out.println(ex.getLocalizedMessage());
				System.out.println(ex.getMessage());
				String error = "Postava  na serveru nenalezena";
				System.out.println(error);
			} catch (IOException ex) {
				String error = ex.getLocalizedMessage();
				System.out.println(error);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		JsonReader jsonReader;
		JsonObject jsonObject = null;
		JsonArray members = null;
		try{
			jsonReader = Json.createReader(is);
			jsonObject = jsonReader.readObject();
			members = jsonObject.getJsonArray("members");
		} catch (JsonParsingException ex) {
			ex.getMessage();
			BufferedReader in = new BufferedReader(new InputStreamReader(is));
			String inputLine;
			try {
				while ((inputLine = in.readLine()) != null)
					System.out.println(inputLine);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		members.forEach(m -> addChar(m));
	}

	private void addChar(JsonValue ch) {
		JsonObject JsonCharacter = (JsonObject) ch;
		int rank = JsonCharacter.getInt("rank");
		JsonCharacter = JsonCharacter.getJsonObject("character");
		if (JsonCharacter.getInt("level") == 100) {
			Character character = new Character(realm, JsonCharacter.getString("name"), rank);
			try {
				dao.createOrUpdate(character);
			} catch (SQLException e) {
				e.printStackTrace();
			}

			//characters.add(new Character(realm, JsonCharacter.getString("name"), rank));
		}
	}

	private void getData() {
		ExecutorService executor = Executors.newFixedThreadPool(16);

		for (Character character : dao) {
			executor.submit(() -> queryAPI(character));
		}
		executor.shutdown();

		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		/*ArrayList<Character> chars = (ArrayList<Character>) characters.clone();
		chars.forEach(this::queryAPI);*/

	}

	private void queryAPI(Character character) {

		InputStream is = null;
		while (is == null) {
			try{
				String host = "http://eu.battle.net/api/";
				URL url = new URL(host + "wow/character/" + character.getRealm() + "/" +  character.getName() +
						"?fields=guild,items,titles,talents,professions");
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setReadTimeout(1500); //1,5 vteřiny

				is = con.getInputStream();
			} catch (FileNotFoundException ex) {
				//System.out.println(ex.getLocalizedMessage());
				//System.out.println(ex.getMessage());
				String error = "Postava " + character.getName() + " na serveru " + character.getRealm() + " nenalezena";
				System.out.println(error);
				try {
					dao.delete(character);
				} catch (SQLException e) {
					e.printStackTrace();
				}
				return;
			} catch (IOException ex) {
				String error = ex.getLocalizedMessage();
				System.out.println("IOEX "+ character.getName() + ": " + error);
				if (error.contains("Read timed out")) {
					timeOuts++;
				}
				Thread.yield();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		// FIX: ošetření, když nejde internet
		JsonReader jsonReader = Json.createReader(is);
		JsonObject jsonObject = jsonReader.readObject();

		// System.out.println(jsonObject.toString());

		JsonObject guild = jsonObject.getJsonObject("guild");
		JsonObject items = jsonObject.getJsonObject("items");
		JsonArray titles = jsonObject.getJsonArray("titles");
		JsonArray talents = jsonObject.getJsonArray("talents");
		JsonObject spec = talents.getJsonObject(0);
		JsonObject specs = spec.getJsonObject("spec");
		if (spec.size() == 7) {
			character.setSpec(specs.getString("name"));

		}else {
			character.setAltSpec(specs.getString("name"));
		}
		spec = talents.getJsonObject(1);
		specs = spec.getJsonObject("spec");
		if (specs != null) {
			if (spec.size() == 7) {
				character.setSpec(specs.getString("name"));
			} else {
				character.setAltSpec(specs.getString("name"));
			}
		}

		JsonObject professions = jsonObject.getJsonObject("professions");
		JsonObject primary_prof = professions.getJsonArray("primary").getJsonObject(0);
		JsonObject secondary_prof = professions.getJsonArray("primary").getJsonObject(1);


		jsonReader.close();

		character.setLvl(jsonObject.getInt("level"));
		character.setPlayerClass(jsonObject.getInt("class"));
		character.setRace(jsonObject.getInt("race"));
		character.setPlayerClass(jsonObject.getInt("class"));
		character.setGender(jsonObject.getInt("gender"));
		character.setAchievementPoints(jsonObject.getInt("achievementPoints"));
		character.setAvatar(jsonObject.getString("thumbnail"));
		character.setGuild(guild.getString("name"));
		character.setIlvl(items.getInt("averageItemLevelEquipped"));
		for (JsonValue i : titles) {
			JsonObject title = (JsonObject) i;
			if (title.size() == 3) {
				character.setTitle(title.getString("name"));
			}
		}
		character.setPrimaryProf(primary_prof.getString("name"));
		character.setPrimaryProfLvl(primary_prof.getInt("rank"));
		character.setSecondaryProf(secondary_prof.getString("name"));
		character.setSecondaryProfLvl(secondary_prof.getInt("rank"));

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

	private void processAudit(JsonObject items, Character ch) {
		int lowerILV = 630;
		int higherIVL = 650;

		JsonObject neck = items.getJsonObject("neck");
		JsonObject back = items.getJsonObject("back");
		JsonObject weapon = items.getJsonObject("mainHand");
		JsonObject ring1 = items.getJsonObject("finger1");
		JsonObject ring2 = items.getJsonObject("finger2");

		if (neck.getInt("itemLevel") >= lowerILV) {
			if (neck.getJsonObject("tooltipParams").getInt("enchant", 0) == 0) {ch.setNeckEnch(false);}
		}
		if (back.getInt("itemLevel") >= lowerILV) {
			if (back.getJsonObject("tooltipParams").getInt("enchant", 0) == 0) {ch.setBackEnch(false);}
		}
		if (ring1.getInt("itemLevel") >= lowerILV) {
			if (ring1.getJsonObject("tooltipParams").getInt("enchant", 0) == 0) {ch.setRing1Ench(false);}
		}
		if (ring2.getInt("itemLevel") >= lowerILV) {
			if (ring2.getJsonObject("tooltipParams").getInt("enchant", 0) == 0) {ch.setRing2Ench(false);}
		}
		if (weapon.getInt("itemLevel") >= higherIVL) {
			if (weapon.getJsonObject("tooltipParams").getInt("enchant", 0) == 0) {ch.setWeaponEnch(false);}
		}
		try {
			dao.createOrUpdate(ch);
		} catch (SQLException e) {
			e.printStackTrace();
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
		if (!result.equals("")) {
			return "<img src=\"img/error.png\" title=\"Chybí: " + result + "\"> " + count;
		}
		return result;
	}


	private void generateHTML() {
		//characters.parallelStream().filter(ch -> ch.getIlvl() > ILVL).forEach(ch -> countRole(ch));
		for (Character ch : dao) {
			if (ch.getIlvl() > ILVL) {
				countRole(ch);
			}
		}


		StringWriter sw = new StringWriter();
		Html html = new Html(sw);

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
			html.raw("<table id=\"myTable\" class=\"table table-striped table-condensed tablesorter\" data-sortlist=\"[[4,1]]\"><thead><tr>" +
					"<th>Jméno</th>" +
					"<th>Povolání</th>" +
					"<th data-placeholder=\"treba heal\">Spec</th>" +
					"<th>Off-Spec</th>" +
					"<th data-value=\">615\">iLVL</th>" +
					"<th>Rank</th>" +
					"<th>Audit</th></tr></thead><tbody>");

			//characters.parallelStream().filter(ch -> ch.getIlvl() > ILVL).forEach(ch -> html.raw(createRow(ch)));
			for (Character ch : dao) {
				if (ch.getIlvl() > ILVL) {
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
		html.p().text("Timeouts: " + timeOuts).end();
		html.script().raw("$(function(){\n" +
				"		$(\"#myTable\").tablesorter(" +
									"{theme: 'dark', widgets: [\"zebra\", \"filter\"],}" +
									");\n" +
					 		  "});").end();
			html.endAll();

		String result = sw.getBuffer().toString();
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
		return ("<tr style=\"color: " + lists.getPClassColor(ch.getPlayerClass()) + "\" ><td>"
				+ "<a href=\"http://eu.battle.net/wow/en/character/" + ch.getRealm() + "/" + ch.getName() +"/advanced\">" + ch.getName() + "</a>"
				+ "</td><td>"
				+ lists.getPClass(ch.getPlayerClass()) + "</td><td>"
				+ "<img src=\"img/" + lists.getRole(ch.getSpec()) + ".png\">" + "<span class=\"role\">" + lists.getRoleType(ch.getSpec()) + lists.getRoleType(ch.getAltSpec()) + "</span> "
				+ ch.getSpec() + "</td><td>"
				+ "<img src=\"img/" + lists.getRole(ch.getAltSpec()) + ".png\">" + "<span class=\"role\">" + lists.getRoleType(ch.getAltSpec()) + lists.getRoleType(ch.getSpec()) + "</span>"
				+ ch.getAltSpec() + "</td><td>" + ch.getIlvl() + "</td><td>" + lists.getRank(ch.getRank()) + "</td>")
				+ "<td>" + getAudit(ch) + "</td>"
				+ "</tr>\n";
	}

	private String getTime(){
		DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		Date today = Calendar.getInstance().getTime();
		String reportDate = df.format(today);
		double cas = (System.nanoTime() - Main.startTime) / 1000000000;

		return "Generováno " + reportDate + ". Export trval " + cas + " s";
	}

	private void countRole(Character ch) {
		if (lists.getRole(ch.getSpec()) == 3) {
			dpss += 1;
		} else if (lists.getRole(ch.getSpec()) == 2) {
			heals += 1;
		} else if (lists.getRole(ch.getSpec()) == 1) {
			tanks += 1;
		}
		if (lists.getRole(ch.getAltSpec()) == 3 && lists.getRole(ch.getSpec()) != lists.getRole(ch.getAltSpec())) {
			adpss += 1;
		}
		if (lists.getRole(ch.getAltSpec()) == 2 && lists.getRole(ch.getSpec()) != lists.getRole(ch.getAltSpec())) {
			aheals += 1;
		}
		if (lists.getRole(ch.getAltSpec()) == 1 && lists.getRole(ch.getSpec()) != lists.getRole(ch.getAltSpec())) {
			atanks += 1;
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
			String sCurrentLine = br.readLine();
				pass = sCurrentLine;
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {

			ssh.authPassword("root", pass);
			final String src = fileName;
			final SFTPClient sftp = ssh.newSFTPClient();
			try {
				sftp.put(new FileSystemFile(src), "/tmp/blog/share/");
			} finally {
				sftp.close();
			}
		} finally {
			ssh.disconnect();
		}
	}
}
