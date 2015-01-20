package cz.kamenitxan.raiders.dataHolders;

import java.io.Serializable;

/**
 * Holds raid kill info.
 * Created by Kamenitxan (kamenitxan@me.com) on 20.01.15.
 */
public class Raid implements Serializable{
	public int id =0;
	public int lfrKills = 0;
	public int normalKills = 0;
	public int heroicKills = 0;
	public int mythicKills = 0;
}
