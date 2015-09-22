package cz.kamenitxan.raiders;

import cz.kamenitxan.raiders.attendance.Attendance;

public class Main {
    public static final double startTime = System.nanoTime();

    /**
     * Example "java -jar Luzanky Thunderhorn"
     * @param args name of guild and server
     */
    public static void main(String[] args) {
		if (args.length > 0) {
			if (args[0].contains("attend")) {
				//new Attendance().parseInput();
			} else {
				new Attendance().getAttendance();
				new Generator().start(args);
			}
		} else {
			new Attendance().getAttendance();
			new Generator().start(args);
		}

    }
}
