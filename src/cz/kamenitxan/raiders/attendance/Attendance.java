package cz.kamenitxan.raiders.attendance;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import cz.kamenitxan.raiders.dataHolders.Character;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.SQLException;
import java.util.Scanner;

public class Attendance {
	private Dao<Character, String> dao = null;

	public Attendance() {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		try {
			final String databaseUrl = "jdbc:sqlite:raiders.db";
			final ConnectionSource connectionSource = new JdbcConnectionSource(databaseUrl);
			dao = DaoManager.createDao(connectionSource, cz.kamenitxan.raiders.dataHolders.Character.class);
			if (!dao.isTableExists()) {
				TableUtils.createTable(connectionSource, Character.class);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void parseInput()  {
		System.out.println("Zadejte tabulku účastníků");
		String line;
		String html = "";
		Scanner stdin = new Scanner(System.in);
		while(stdin.hasNextLine() && !( line = stdin.nextLine() ).equals( "" ))
		{
			html+=line;
		}
		stdin.close();
		try {
			processMembers(html);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println("Účast zaznamenána");
	}

	private void processMembers(String html) throws SQLException {
		Document doc = Jsoup.parse(html);
		Elements attended = doc.select(".composition-entry a");
		for (Character ch : dao) {
			boolean done = false;
			for (Element a : attended) {
				if (ch.getName().equals(a.text())) {
					ch.setAttendanceHistory(true);
					done = true;
					System.out.println(ch.getName());
					break;
				}
			}
			if (!done) {
				ch.setAttendanceHistory(false);
			}
			dao.update(ch);
		}
	}
}