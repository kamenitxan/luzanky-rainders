package cz.kamenitxan.raiders.attendance;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import cz.kamenitxan.raiders.Generator;
import cz.kamenitxan.raiders.dataHolders.Character;
import cz.kamenitxan.raiders.dataHolders.ProcessedRaids;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@SuppressWarnings("HardcodedFileSeparator")
public class Attendance {
	private static final String APIKey = "24830eee747817b10ec6d21434cb94d5";
	private Dao<Character, String> characterDao = null;
	private Dao<ProcessedRaids, String> pRaidDao = null;
	private List<String> raidToProcess = new ArrayList<>();


	public Attendance() {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		try {
			final String databaseUrl = "jdbc:sqlite:raiders.db";
			final ConnectionSource connectionSource = new JdbcConnectionSource(databaseUrl);
			characterDao = DaoManager.createDao(connectionSource, Character.class);
			pRaidDao = DaoManager.createDao(connectionSource, ProcessedRaids.class);
			if (!characterDao.isTableExists()) {
				TableUtils.createTable(connectionSource, Character.class);
			}
			if (!pRaidDao.isTableExists()) {
				TableUtils.createTable(connectionSource, ProcessedRaids.class);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void getAttendance() {
		InputStream is = null;
		final String host = "https://www.warcraftlogs.com:443/v1/reports/guild/";
		while (is == null) {
			try {
				final URL url = new URL(host + Generator.guildName + "/" + Generator.realm + "/EU?api_key=" + APIKey);
				final HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestProperty("Accept-Encoding", "gzip, deflate");
				con.setReadTimeout(1500); //1,5 vteřiny

				is = new GZIPInputStream(con.getInputStream());
			} catch (FileNotFoundException ex) {
				System.out.println(ex.getLocalizedMessage());
				System.out.println(ex.getMessage());
				final String error = "Guilda  na serveru nenalezena";
				System.out.println(error);
			} catch (IOException ex) {
				System.out.println(ex.getLocalizedMessage());
				System.exit(1);
			}
		}
		JsonArray raids;
		try{
			raids = Json.createReader(is).readArray();
			List<String> ids = new ArrayList<>();
			for (JsonValue rv : raids) {
				JsonObject r = (JsonObject) rv;
				ids.add(r.getString("id"));
			}
			getRaidsToProcess(ids);
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

		raidToProcess.forEach(this::processRaid);

	}

	private void getRaidsToProcess(List<String> ids) {
		try {
			ids = ids.subList(ids.size()-15, ids.size());
			List<String> doneRaid = pRaidDao.queryForAll().stream().map(ProcessedRaids::toString).collect(Collectors.toList());
			List<String> temp = ids.stream().filter(id -> !doneRaid.contains(id)).collect(Collectors.toList());
			/*List<String> temp = new ArrayList<>();
			for (String id : ids) {
				if (!doneRaid.contains(id)) {
					temp.add(id);
				}
			}*/
			raidToProcess.addAll(temp);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void processRaid(String id) {
		InputStream is = null;
		final String host = "https://www.warcraftlogs.com:443/v1/report/fights/";
		while (is == null) {
			try {
				final URL url = new URL(host + id + "?api_key=" + APIKey);
				final HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestProperty("Accept-Encoding", "gzip, deflate");
				con.setReadTimeout(1500); //1,5 vteřiny

				is = new GZIPInputStream(con.getInputStream());
			} catch (FileNotFoundException ex) {
				System.out.println(ex.getLocalizedMessage());
				System.out.println(ex.getMessage());
				final String error = "report  na serveru nenalezen";
				System.out.println(error);
			} catch (IOException ex) {
				System.out.println(ex.getLocalizedMessage());
				System.exit(1);
			}
		}
		JsonArray members;
		try{
			members = Json.createReader(is).readObject().getJsonArray("friendlies");
			List<String> names = new ArrayList<>();
			for (JsonValue jv : members) {
				JsonObject m = (JsonObject) jv;
				if (!Objects.equals(m.getString("type"), "NPC")) {
					names.add(m.getString("name"));
				}
			}
			processMembers(names);
			pRaidDao.createIfNotExists(new ProcessedRaids(id));

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
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void processMembers(List<String> names) throws SQLException {
		System.out.println("RAID JMENA\n");
		for (Character ch : characterDao) {
			boolean done = false;
			for (String n : names) {
				if (ch.getName().equals(n)) {
					ch.setAttendanceHistory(true);
					done = true;
					System.out.println(ch.getName());
					break;
				}
			}
			if (!done) {
				ch.setAttendanceHistory(false);
			}
			characterDao.update(ch);
		}
	}
}

//https://www.warcraftlogs.com:443/v1/reports/guild/Luzanky/Thunderhorn/EU?api_key=24830eee747817b10ec6d21434cb94d5
//https://www.warcraftlogs.com:443/v1/reports/guild/Luzanky/Thunderhorn?api_key=24830eee747817b10ec6d21434cb94d5