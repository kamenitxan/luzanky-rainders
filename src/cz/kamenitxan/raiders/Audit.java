package cz.kamenitxan.raiders;


import cz.kamenitxan.raiders.dataHolders.Character;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.ArrayList;

public class Audit {

    /**
     * Checks if character have requested items like gems and enchants
     * @param items list of equiped items
     * @param ch checked character
     */
    public static void processAudit(JsonObject items, cz.kamenitxan.raiders.dataHolders.Character ch) {
        final int lowerILV = 630;
        final int higherIVL = 650;
        ch.setMissingGems(false);

        final JsonObject head = items.getJsonObject("head");
        if (head.getInt("itemLevel") >= lowerILV) {
            checkGem(ch, head);
        }
        final JsonObject neck = items.getJsonObject("neck");
        if (neck.getInt("itemLevel") >= lowerILV) {
            if (neck.getJsonObject("tooltipParams").getInt("enchant", 0) == 0) {ch.setNeckEnch(false);}
            else {ch.setNeckEnch(true);}
            checkGem(ch, neck);
        }
        final JsonObject shoulder = items.getJsonObject("shoulder");
        if (shoulder.getInt("itemLevel") >= lowerILV) {
            checkGem(ch, shoulder);
        }
        final JsonObject back = items.getJsonObject("back");
        if (back.getInt("itemLevel") >= lowerILV) {
            if (back.getJsonObject("tooltipParams").getInt("enchant", 0) == 0) {ch.setBackEnch(false);}
            else {ch.setBackEnch(true);}
            checkGem(ch, back);
        }
        final JsonObject chest = items.getJsonObject("chest");
        if (chest.getInt("itemLevel") >= lowerILV) {
            checkGem(ch, chest);
        }
        final JsonObject wrist = items.getJsonObject("wrist");
        if (wrist.getInt("itemLevel") >= lowerILV) {
            checkGem(ch, wrist);
        }
        final JsonObject hands = items.getJsonObject("hands");
        if (hands.getInt("itemLevel") >= lowerILV) {
            checkGem(ch, hands);
        }
        final JsonObject waist = items.getJsonObject("waist");
        if (waist.getInt("itemLevel") >= lowerILV) {
            checkGem(ch, waist);
        }
        final JsonObject legs = items.getJsonObject("legs");
        if (legs.getInt("itemLevel") >= lowerILV) {
            checkGem(ch, legs);
        }
        final JsonObject feet = items.getJsonObject("feet");
        if (feet.getInt("itemLevel") >= lowerILV) {
            checkGem(ch, feet);
        }
        final JsonObject ring1 = items.getJsonObject("finger1");
        if (ring1.getInt("itemLevel") >= lowerILV) {
            if (ring1.getJsonObject("tooltipParams").getInt("enchant", 0) == 0) {ch.setRing1Ench(false);}
            else {ch.setRing1Ench(true);}
            checkGem(ch, ring1);
        }
        final JsonObject ring2 = items.getJsonObject("finger2");
        if (ring2.getInt("itemLevel") >= lowerILV) {
            if (ring2.getJsonObject("tooltipParams").getInt("enchant", 0) == 0) {ch.setRing2Ench(false);}
            else {ch.setRing2Ench(true);}
            checkGem(ch, ring2);
        }
        final JsonObject trinket1 = items.getJsonObject("trinket1");
        if (trinket1.getInt("itemLevel") >= lowerILV) {
            checkGem(ch, trinket1);
        }
        final JsonObject trinket2 = items.getJsonObject("trinket2");
        if (trinket2.getInt("itemLevel") >= lowerILV) {
            checkGem(ch, trinket2);
        }
        final JsonObject weapon = items.getJsonObject("mainHand");
        if (weapon.getInt("itemLevel") >= higherIVL) {
            if (weapon.getJsonObject("tooltipParams").getInt("enchant", 0) == 0) {ch.setWeaponEnch(false);}
            else {ch.setWeaponEnch(true);}
            checkGem(ch, weapon);
        }
//        try {
//            dao.createOrUpdate(ch);
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
    }

    /**
     * Checks if item contains gem
     * @param ch checked character
     * @param item checked item
     */
    private static void checkGem(Character ch, JsonObject item) {
        if (item.getJsonArray("bonusLists").size() >= 1) {
            // 563, 564, 565
            ArrayList<Integer> sockets = new ArrayList<>();
            sockets.add(523);
            sockets.add(563);
            sockets.add(564);
            sockets.add(565);
            JsonArray bonusy =  item.getJsonArray("bonusLists");
            if (bonusy != null) {
                bonusy.stream().filter(bonus -> sockets.contains(Integer.valueOf(bonus.toString()))).forEach(bonus -> {
                    if (item.getJsonObject("tooltipParams").getInt("gem0", 0) == 0) {
                        ch.setMissingGems(true);
                    }
                });
            }
        }
    }

    /**
     *
     * @param ch checked character
     * @return audit results
     */
    public static String getAudit(Character ch) {
        String result = "";
        int count = 0;
        if (!ch.isBackEnch()) {
            result += "Back enchant, ";
            count++;
        }
        if (!ch.isNeckEnch()) {
            result += "Neck enchant, ";
            count++;
        }
        if (!ch.isWeaponEnch()) {
            result += "Weapon enchant, ";
            count++;
        }
        if (!ch.isRing1Ench()) {
            result += "Ring1 enchant, ";
            count++;
        }
        if (!ch.isRing2Ench()) {
            result += "Ring2 enchant, ";
            count++;
        }
        if (ch.getMissingGems() > 0) {
            result += "Chybí " + ch.getMissingGems() + " gem(y), ";
            count += ch.getMissingGems();
        }
        if (!result.equals("")) {
            return "<img src=\"img/error.png\" title=\"Chybí: " + result + "\"> " + count;
        }
        return result;
    }

}
