package cz.kamenitxan.raiders.attendance;

import java.io.Serializable;
import java.util.ArrayList;


public class AttendanceHistory implements Serializable {
	private static final long serialVersionUID = 1L;

	private ArrayList<Boolean> history = new ArrayList<Boolean>() {{
		for (int i = 1; i <= 15; i++) {
			add(false);
		}
	}};

	public void addAttendace(boolean attended) {
		history.remove(0);
		if (attended) {
			history.add(true);
		} else {
			history.add(false);
		}
	}

	public ArrayList<Boolean> getHistory() {
		return history;
	}
}
