package cz.kamenitxan;

import java.io.Serializable;

public class Spec implements Serializable {
	private String spec = "";
	private int ilvl = 0;

	public String getSpec() {
		return spec;
	}

	public void setSpec(String spec) {
		this.spec = spec;
	}

	public int getIlvl() {
		return ilvl;
	}

	public void setIlvl(int ilvl) {
		this.ilvl = ilvl;
	}
}
