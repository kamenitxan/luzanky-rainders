package cz.kamenitxan.raiders.attendance;

import java.io.Serializable;
import java.util.ArrayList;


public class AttendanceHistory implements Serializable {
	private ArrayList<Boolean> history = new ArrayList<Boolean>() {{
		for (int i = 1; i <= 15; i++) {
			add(false);
		}
	}};

	public void addAttendace() {
		history.remove(0);
		history.add(true);
	}

	public ArrayList<Boolean> getHistory() {
		return history;
	}
}
