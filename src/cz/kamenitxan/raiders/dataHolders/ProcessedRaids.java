package cz.kamenitxan.raiders.dataHolders;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by Kamenitxan (kamenitxan@me.com) on 22.09.15.
 */
@DatabaseTable
public class ProcessedRaids {
	@DatabaseField(id = true)
	private String id;

	public ProcessedRaids() {
	}

	public ProcessedRaids(String id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ProcessedRaids that = (ProcessedRaids) o;

		return id.equals(that.id);

	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return id;
	}
}
