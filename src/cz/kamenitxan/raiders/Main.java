package cz.kamenitxan.raiders;

public class Main {
    public static final double startTime = System.nanoTime();

    /**
     * Example "java -jar Luzanky Thunderhorn"
     * @param args name of guild and server
     */
    public static void main(String[] args) {
        new Generator().start(args);
    }
}
