package cz.kamenitxan.raiders.dataHolders;

import java.util.HashMap;
import java.util.Map;

public class Lists {
	private static Lists singleton = new Lists();
	private Map<Integer, Povolani> classes;


	static class Povolani {
		public String name;
		public String color;
		public Povolani(String name, String color) {
			this.name = name;
			this.color = color;
		}
	}

	private Lists(){
		classes = new HashMap<>();
		classes.put(1, new Povolani("Warrior", "#C79C6E"));
		classes.put(2, new Povolani("Paladin", "#F58CBA"));
		classes.put(3, new Povolani("Hunter", "#ABD473"));
		classes.put(4, new Povolani("Rogue", "#FFF569"));
		classes.put(5, new Povolani("Priest", "#FFFFFF"));
		classes.put(6, new Povolani("Death Knight", "#C41F3B"));
		classes.put(7, new Povolani("Shaman", "#0070DE"));
		classes.put(8, new Povolani("Mage", "#69CCF0"));
		classes.put(9, new Povolani("Warlock", "#9482C9"));
		classes.put(10, new Povolani("Monk", "#00FF96"));
		classes.put(11, new Povolani("Druid", "#FF7D0A"));

	}

	/**
	 * @return singleton of Lists
	 */
	public static Lists getInstance() {
		return singleton;
	}

	/**
	 * Gets class name from API id
	 * @param i id
	 * @return class name
	 */
	public String getPClass(int i){
		return classes.get(i).name;
	}

    /**
     * Returns class color
     * @param i class id from api
     * @return hex color
     */
	public String getPClassColor(int i){
		return classes.get(i).color;
	}

    /**
     * @param rank number from api
     * @return rank of Luzanky guild
     */
	public String getRank(int rank) {
		switch (rank){
			case 1: return "Officer";
			case 2: return "Bankéř";
			case 3: return "Raid leader";
			case 4: return "Honorary";
			case 5: return "Veteran";
			case 6: return "Raider";
			case 7: return "Member";
			case 8: return "Alt";
			case 9: return "Recruit";
			default: return "nulla";
		}
	}

	/**
	 *
	 * @param spec spec name from API
	 * @return 1 tank, 2 heal 3 dmg
	 */
	public static int getRole(String spec) {
		if (spec == null || spec.equals("")) {
			return 0;
		}
		if (spec.equals("Guardian")) {
			return 1;
		}
		if (spec.equals("Restoration")) {
			return 2;
		}
		if (spec.equals("Blood")) {
			return 1;
		}
		if (spec.equals("Brewmaster")) {
			return 1;
		}
		if (spec.equals("Mistweaver")) {
			return 2;
		}
		if (spec.equals("Holy")) {
			return 2;
		}
		if (spec.equals("Protection")) {
			return 1;
		}
		if (spec.equals("Discipline")) {
			return 2;
		}
		if (spec.equals("Protection")) {
			return 1;
		}
		return 3;
	}

    /**
     * @param spec name from API
     * @return type of role: tank, heal or dps
     */
	public String getRoleType(String spec) {
		final int role = getRole(spec);
		if (role == 1) {return "tank";}
		if (role == 2) {return "heal";}
		if (role == 3) {return "dps";}
		return "";
	}

	/**
	 * Checks if char is someones alt. Hardcoded for now.
	 * @param charName name of character
	 * @return name of main + alt name
	 */
	public static String altsMain(String charName) {
		Map<String, String> alts = new HashMap<String, String>() {{
			put("Stribrovous", "Reve");
			put("Fandacz", "Frantacz");
			put("Werhaf", "Frantacz");
			put("Turmi", "Turm");
			put("Chlupatice", "Padouch");
			put("Horyn", "Homir");
			put("Padou", "Padouch");
			put("Dredgart", "Yagwin");
			put("Misantrop", "Mellber");
			put("Huntraz", "Raznikcz");
			put("Asparuch", "Rozverná");
			put("Amfitríta", "Mellber");
			put("Silmë", "Icesky");
			put("Alasseon","Mellber");
			put("Metalmell","Mellber");
			put("Yagwinna","Yagwin");
			put("Alfar","Nysse");
			put("Pierette","Clairette");
		}};
		if (alts.containsKey(charName)) {
			return alts.get(charName) + " (" + charName + ")";
		}else {
			return charName;
		}
	}

}
