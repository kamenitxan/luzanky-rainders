package cz.kamenitxan;

import com.googlecode.jatl.Html;
import javax.json.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
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
	private ArrayList<Character> characters;
	private final Lists lists = Lists.getInstance();
	private final int ILVL = 500;

	private int tanks = 0;
	private int heals = 0;
	private int dpss = 0;
	private int atanks = 0;
	private int aheals = 0;
	private int adpss = 0;

	private int timeOuts = 0;

	public Generator(ArrayList<Character> characters) {
		this.characters = characters;
	}

	public void getData() {
		ExecutorService executor = Executors.newFixedThreadPool(16);

		for (Character character : characters) {
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

		generateHTML();

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
				characters.remove(character);
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

		System.out.println(jsonObject.toString());

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
	}

	private void processAudit(JsonObject items, Character ch) {
		JsonObject neck = items.getJsonObject("neck").getJsonObject("tooltipParams");
		JsonObject back = items.getJsonObject("back").getJsonObject("tooltipParams");
		JsonObject weapon = items.getJsonObject("mainHand");
		JsonObject ring1 = items.getJsonObject("finger1").getJsonObject("tooltipParams");
		JsonObject ring2 = items.getJsonObject("finger2").getJsonObject("tooltipParams");

		if (neck.getInt("enchant", 0) == 0) {ch.setNeckEnch(false);}
		if (back.getInt("enchant", 0) == 0) {ch.setBackEnch(false);}
		if (ring1.getInt("enchant", 0) == 0) {ch.setRing1Ench(false);}
		if (ring2.getInt("enchant", 0) == 0) {ch.setRing2Ench(false);}
		if (weapon.getInt("itemLevel") >= 640) {
			if (weapon.getJsonObject("tooltipParams").getInt("enchant", 0) == 0) {ch.setWeaponEnch(false);}
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
		if (!ch.isRing1Ench() || !ch.isRing2Ench()) {
			result += "Ring enchant, ";
			count++;
		}
		if (!result.equals("")) {
			return "<img src=\"img/error.png\" title=\"Chybí: " + result + "\"> " + count;
		}
		return result;
	}


	private void generateHTML() {
		characters.parallelStream().filter(ch -> ch.getIlvl() > ILVL).forEach(ch -> countRole(ch));

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

			characters.parallelStream().filter(ch -> ch.getIlvl() > ILVL).forEach(ch -> html.raw(createRow(ch)));
//			for (Character ch : characters) {
//				if (ch.getIlvl() > ILVL) {
//					html.raw(createRow(ch));
//				}
//			}

			html.raw("</tbody></table>");
			html.p().text("Tanků: " + tanks + " (" + (tanks + atanks) + ")").br()
					.text("Healů: " + heals + " (" + (heals + aheals) + ")").br()
					.text("DPS: "   + dpss + " (" +  (dpss  + adpss)  + ")").br()
					.text("Celkem: " + characters.size());

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
}
