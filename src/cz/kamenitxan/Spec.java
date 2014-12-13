package cz.kamenitxan;

import java.io.Serializable;

public class Spec implements Serializable {
	private String spec = "";
	private int ilvl = 0;
	private boolean active = false;

	public String getSpecName() {
		return spec;
	}

	public void setSpec(String spec) {
		this.spec = spec;
	}
	public void setSpec(String spec, boolean active) {
		this.spec = spec;
		this.active = active;
	}

	public int getIlvl() {
		return ilvl;
	}

	public void setIlvl(int ilvl) {
		this.ilvl = ilvl;
	}

	public boolean isActive() {
		return active;
	}
}
