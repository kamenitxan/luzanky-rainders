package cz.kamenitxan;

import javax.json.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

public class Main {
    private static final String guildName = "Luzanky";
    private static final String realm = "Thunderhorn";
    public static final double startTime = System.nanoTime();

    private static ArrayList<Character> characters = new ArrayList<>();

    public static void main(String[] args) {
        InputStream is = null;
        try{
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

        JsonReader jsonReader = Json.createReader(is);
        JsonObject jsonObject = jsonReader.readObject();
        JsonArray members = jsonObject.getJsonArray("members");

        for (JsonValue chars : members) {
            JsonObject character = (JsonObject) chars;
            int rank = character.getInt("rank");
            character = character.getJsonObject("character");
            if (character.getInt("level") == 100) {
                characters.add(new Character(realm, character.getString("name"), rank));
            }
        }

        System.out.println(jsonObject.toString());

        new Generator(characters).getData();
    }

    public ArrayList<Character> getCharacters() {
        return characters;
    }
}
