package cz.kamenitxan.raiders;

import cz.kamenitxan.raiders.attendance.Attendance;

public class Main {
    public static final double startTime = System.nanoTime();
	public static boolean sshUpload = false;
	public static String apiKey = "";

    /**
     * Example "java -jar Luzanky Thunderhorn"
     * @param args name of guild and server
     */
    public static void main(String[] args) {
		if (args.length > 0) {
			for (String arg : args) {
				if (arg.contains("-apiKey=")) {
					arg = arg.replace("-apiKey=", "");
					apiKey = arg;
				}
				if (arg.contains("-upload")) {
					sshUpload = true;
				}
			}
		}
		new Attendance().getAttendance();
		new Generator().start(args);
    }
}
