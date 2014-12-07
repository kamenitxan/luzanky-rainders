package cz.kamenitxan;

import javax.json.*;
import javax.json.stream.JsonParsingException;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;

public class Main {
    private static String guildName = "Luzanky";
    private static String realm = "Thunderhorn";
    public static final double startTime = System.nanoTime();

    private static ArrayList<Character> characters = new ArrayList<>();

    /**
     * Example "java -jar Luzanky Thunderhorn"
     * @param args name of guild and server
     */
    public static void main(String[] args) {
        if (args.length != 0) {
            guildName = args[0];
            realm = args[1];
        }

        System.out.println("Běh zahájen");
        InputStream is = null;
        while (is == null) {
            try {
                String host = "http://eu.battle.net/api/";
                URL url = new URL(host + "wow/guild/" + realm + "/" + guildName +
                        "?fields=members");
                is = url.openStream();
            } catch (FileNotFoundException ex) {
                System.out.println(ex.getLocalizedMessage());
                System.out.println(ex.getMessage());
                String error = "Postava  na serveru nenalezena";
                System.out.println(error);
            } catch (IOException ex) {
                String error = ex.getLocalizedMessage();
                System.out.println(error);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        JsonReader jsonReader;
        JsonObject jsonObject = null;
        try{
            jsonReader = Json.createReader(is);
            jsonObject = jsonReader.readObject();
        } catch (JsonParsingException ex) {
            ex.getMessage();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String inputLine;
            try {
                while ((inputLine = in.readLine()) != null)
					System.out.println(inputLine);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        JsonArray members = jsonObject.getJsonArray("members");

        for (JsonValue v : members) {
            addChar(v);
        }

        //members.forEach(m -> addChar(m));

        System.out.println(jsonObject.toString());

        new Generator(characters).getData();
    }
    public static String run() {
        System.out.println("Běh zahájen");
        InputStream is = null;
        while (is == null) {
            try {
                String host = "http://eu.battle.net/api/";
                URL url = new URL(host + "wow/guild/" + realm + "/" + guildName +
                        "?fields=members");
                is = url.openStream();
            } catch (FileNotFoundException ex) {
                System.out.println(ex.getLocalizedMessage());
                System.out.println(ex.getMessage());
                String error = "Postava  na serveru nenalezena";
                System.out.println(error);
            } catch (IOException ex) {
                String error = ex.getLocalizedMessage();
                System.out.println(error);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        JsonReader jsonReader;
        JsonObject jsonObject = null;
        try{
            jsonReader = Json.createReader(is);
            jsonObject = jsonReader.readObject();
        } catch (JsonParsingException ex) {
            ex.getMessage();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String inputLine;
            try {
                while ((inputLine = in.readLine()) != null)
                    System.out.println(inputLine);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        JsonArray members = jsonObject.getJsonArray("members");

        for (JsonValue v : members) {
            addChar(v);
        }

        //members.forEach(m -> addChar(m));

        //return jsonObject.toString();

        return new Generator(characters).getData();
    }

    private static void addChar(JsonValue ch) {
        JsonObject character = (JsonObject) ch;
        int rank = character.getInt("rank");
        character = character.getJsonObject("character");
        if (character.getInt("level") == 100) {
            characters.add(new Character(realm, character.getString("name"), rank));
        }
    }

    public ArrayList<Character> getCharacters() {
        return characters;
    }
}
