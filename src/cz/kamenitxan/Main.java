package cz.kamenitxan;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;

import javax.json.*;
import javax.json.stream.JsonParsingException;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;

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
