package cz.kamenitxan;


import javax.json.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Generator {
	private ArrayList<Character> characters;
	private final Lists lists = Lists.getInstance();

	private int tanks = 0;
	private int heals = 0;
	private int dpss = 0;
	private int atanks = 0;
	private int aheals = 0;
	private int adpss = 0;

	public Generator(ArrayList<Character> characters) {
		this.characters = characters;
	}

	public void getData() {
		ExecutorService executor = Executors.newFixedThreadPool(16);

		for (Character character : characters) {
			executor.submit(new Runnable() {
				@Override
				public void run() {
					queryAPI(character);
				}
			});
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
				is = url.openStream();
			} catch (FileNotFoundException ex) {
				//System.out.println(ex.getLocalizedMessage());
				//System.out.println(ex.getMessage());
				String error = "Postava " + character.getName() + " na serveru " + character.getRealm() + " nenalezena";
				System.out.println(error);
				characters.remove(character);
				return;
			} catch (IOException ex) {
				String error = ex.getLocalizedMessage();
				System.out.println("IOEX: " + error);
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

		//System.out.println(jsonObject.toString());

		if (character.getTitle() == null) {
			character.setTitle("");
		}
	}


	private void generateHTML() {
		Collections.sort(characters);
		Collections.reverse(characters);
		StringBuilder sb = new StringBuilder();
		sb.append("<html><head><meta charset=\"UTF-8\"><title>Raiders of Luzanky</title>" +
				"<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css\">" +
				"<style type=\"text/css\">.table-striped>tbody>tr:nth-child(odd) {background-color: rgb(28, 28, 28) !important;} " +
				".table {width: auto;} </style>" +
				"</head>" +
				"<body style=\"color: white; background-color: black; \">");
		sb.append("<h1>Seznam raiderů Lužánek</h1><table class=\"table table-striped table-condensed\">");
		sb.append("<thead><tr><td>Jméno</td><td>Povolání</td><td>Spec</td><td>Off-Spec</td><td>iLVL</td><td>Rank</td></tr></thead>");
		for (Character ch : characters) {
			if (ch.getIlvl() > 615) {
				if (lists.getRole(ch.getSpec()) == 1) {
					tanks += 1;
				}
				if (lists.getRole(ch.getSpec()) == 2) {
					heals += 1;
				}
				if (lists.getRole(ch.getSpec()) == 3) {
					dpss += 1;
				}
				if (lists.getRole(ch.getAltSpec()) == 1) {
					atanks += 1;
				}
				if (lists.getRole(ch.getAltSpec()) == 2) {
					aheals += 1;
				}
				if (lists.getRole(ch.getAltSpec()) == 3) {
					adpss += 1;
				}
			}
			if (ch.getIlvl() > 615) {
				sb.append("<tr style=\"color: " + lists.getPClassColor(ch.getPlayerClass()) + "\" ><td>"
						+ ch.getName()
						+ "</td><td>"
						+ lists.getPClass(ch.getPlayerClass()) + "</td><td>"
						+ "<img src=\"img/" + lists.getRole(ch.getSpec()) + ".png\"> "
						+ ch.getSpec() + "</td><td>"
						+ "<img src=\"img/" + lists.getRole(ch.getAltSpec()) + ".png\"> "
						+ ch.getAltSpec() + "</td><td>" + ch.getIlvl() + "</td><td>" + lists.getRank(ch.getRank()) + "</td>") ;
				//System.out.println(ch.getName());
			}
		}
		sb.append("</table><p>Tanků: " + tanks + " (" + (tanks + atanks) + ")<br>" +
				"Healů: " + heals + " (" + (heals + aheals) + ")<br>" +
				"DPS: " + dpss + " (" + (dpss + adpss) +")</p>" +
				"</body></html>");


		DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		Date today = Calendar.getInstance().getTime();
		String reportDate = df.format(today);

		double cas = (System.nanoTime() - Main.startTime) / 1000000000;

		sb.append("<p>Generováno " + reportDate + ". Export trval " + cas + " s");
		try {
			Files.write(Paths.get("raiders.html"), sb.toString().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("HTML vygenerováno");
	}
}