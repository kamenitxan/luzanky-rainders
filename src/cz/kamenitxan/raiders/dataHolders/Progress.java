package cz.kamenitxan.raiders.dataHolders;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds character raids progress
 * Created by Kamenitxan (kamenitxan@me.com) on 20.01.15.
 */
public class Progress implements Serializable{
	Map<Integer, Raid> raids = null;

	public Progress() {
		raids = new HashMap<>();
		raids.put(32, new Raid() {{
			id = 32;
		}});
		raids.put(33, new Raid() {{
			id = 33;
		}});
	}

	public Raid getRaid(int raid) {
		return raids.get(raid);
	}

	void setRaidProgress(int id, int lfr, int normal, int heroic, int mythic) {
		Raid raid = raids.get(id);
		raid.lfrKills = lfr;
		raid.normalKills = normal;
		raid.heroicKills = heroic;
		raid.mythicKills = mythic;
	}
}
