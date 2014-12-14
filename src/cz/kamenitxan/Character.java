package cz.kamenitxan;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public class Character implements Comparable{
	@DatabaseField(id = true)
	private String name;
	@DatabaseField
	private String realm;
	@DatabaseField
	private String avatar = null;
	@DatabaseField
	private String guild = "";
	@DatabaseField
	private String title = null;
	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Spec spec = null;
	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Spec altSpec = null;
	@DatabaseField
	private String primaryProf = null;
	@DatabaseField
	private int primaryProfLvl = 0;
	@DatabaseField
	private String secondaryProf = null;
	@DatabaseField
	private int secondaryProfLvl = 0;
	@DatabaseField
	private int lvl = 0;
	@DatabaseField
	private int rank = 0;
	@DatabaseField
	private int playerClass = 0;
	@DatabaseField
	private int race = 0;
	@DatabaseField
	private int gender = 0;
	@DatabaseField
	private int achievementPoints = 0;
	@DatabaseField
	private boolean weaponEnch = true;
	@DatabaseField
	private boolean neckEnch = true;
	@DatabaseField
	private boolean backEnch = true;
	@DatabaseField
	private boolean ring1Ench = true;
	@DatabaseField
	private boolean ring2Ench = true;
	@DatabaseField
	private int missingGems = 0;
	@DatabaseField
	private int dpsChallenge = 0;
	@DatabaseField
	private int tankChallenge = 0;
	@DatabaseField
	private int healChallenge = 0;
	@DatabaseField
	private int lastModified = 0;

	public Character() {}

	public Character(String realm, String name, int rank) {
		this.realm = realm;
		this.name = name;
		this.rank = rank;
		this.spec = new Spec();
		this.altSpec = new Spec();
	}

	public int getRace() {
		return race;
	}

	public void setRace(int race) {
		this.race = race;
	}

	public int getAchievementPoints() {
		return achievementPoints;
	}

	public void setAchievementPoints(int achievementPoints) {
		this.achievementPoints = achievementPoints;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}

	public int getRank() {
		return rank;
	}

	public int getLvl() {
		return lvl;
	}

	public void setLvl(int lvl) {
		this.lvl = lvl;
	}

	public int getPlayerClass() {
		return playerClass;
	}

	public void setPlayerClass(int playerClass) {
		this.playerClass = playerClass;
	}

	public int getGender() {
		return gender;
	}

	public void setGender(int gender) {
		this.gender = gender;
	}

	public String getGuild() {
		return guild;
	}

	public void setGuild(String guild) {
		this.guild = guild;
	}

	public int getIlvl() {
		return spec.getIlvl();
	}

	public void setIlvl(int ilvl) {
		this.spec.setIlvl(ilvl);
	}
	public int getAltIlvl() {
		return altSpec.getIlvl();
	}

	public void setAltIlvl(int ilvl) {
		this.altSpec.setIlvl(ilvl);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Spec getSpec() {
		return spec;
	}

	public void setSpec(String spec) {
		this.spec.setSpec(spec);
	}
	public void setSpec(String spec, boolean active) {
		this.spec.setSpec(spec, active);
	}

	public Spec getAltSpec() {
		return altSpec;
	}

	public void setAltSpec(String altSpec) {
		this.altSpec.setSpec(altSpec);
	}
	public void setAltSpec(String spec, boolean active) {
		this.altSpec.setSpec(spec, active);
	}

	public String getPrimaryProf() {
		return primaryProf;
	}

	public void setPrimaryProf(String primaryProf) {
		this.primaryProf = primaryProf;
	}

	public String getSecondaryProf() {
		return secondaryProf;
	}

	public void setSecondaryProf(String secondaryProf) {
		this.secondaryProf = secondaryProf;
	}

	public int getPrimaryProfLvl() {
		return primaryProfLvl;
	}

	public void setPrimaryProfLvl(int primaryProfLvl) {
		this.primaryProfLvl = primaryProfLvl;
	}

	public int getSecondaryProfLvl() {
		return secondaryProfLvl;
	}

	public void setSecondaryProfLvl(int secondaryProfLvl) {
		this.secondaryProfLvl = secondaryProfLvl;
	}

	public boolean isWeaponEnch() {
		return weaponEnch;
	}

	public void setWeaponEnch(boolean weaponEnch) {
		this.weaponEnch = weaponEnch;
	}

	public boolean isNeckEnch() {
		return neckEnch;
	}

	public void setNeckEnch(boolean neckEnch) {
		this.neckEnch = neckEnch;
	}

	public boolean isBackEnch() {
		return backEnch;
	}

	public void setBackEnch(boolean backEnch) {
		this.backEnch = backEnch;
	}

	public boolean isRing1Ench() {
		return ring1Ench;
	}

	public void setRing1Ench(boolean ring1Ench) {
		this.ring1Ench = ring1Ench;
	}

	public boolean isRing2Ench() {
		return ring2Ench;
	}

	public void setRing2Ench(boolean ring2Ench) {
		this.ring2Ench = ring2Ench;
	}

	public int getMissingGems() {
		return missingGems;
	}

	public void setMissingGems(boolean missingGem) {
		if (missingGem) {
			this.missingGems += 1;
		}else {
			this.missingGems = 0;
		}
	}

	public int getDpsChallenge() {
		return dpsChallenge;
	}

	public void setDpsChallenge(int dpsChallenge) {
		this.dpsChallenge = dpsChallenge;
	}

	public int getTankChallenge() {
		return tankChallenge;
	}

	public void setTankChallenge(int tankChallenge) {
		this.tankChallenge = tankChallenge;
	}

	public int getHealChallenge() {
		return healChallenge;
	}

	public void setHealChallenge(int healChallenge) {
		this.healChallenge = healChallenge;
	}

	/**
	 * @param s 1 is main spec, 2 is off spec
	 * @return level of proving ground for spec
	 */
	public int getSpecChallenge(int s) {
		if (s == 1) {
			s = Lists.getRole(spec.getSpecName());
		}else {
			s = Lists.getRole(altSpec.getSpecName());
		}
		switch (s) {
			case 3: {return dpsChallenge;}
			case 1: {return tankChallenge;}
			case 2: {return healChallenge;}
		}
		return 0;
	}

	public int getLastModified() {
		return lastModified;
	}

	public void setLastModified(int lastModified) {
		this.lastModified = lastModified;
	}

	@Override
	public int compareTo(Object o) {
		if (spec.getIlvl() < ((Character) o).getIlvl() ){
			return -1;
		}
		if (spec.getIlvl() == ((Character) o).getIlvl()) {
			return 0;
		}else {
			return 1;
		}

	}


}
