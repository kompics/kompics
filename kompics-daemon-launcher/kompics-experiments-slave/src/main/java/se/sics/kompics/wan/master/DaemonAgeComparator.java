package se.sics.kompics.wan.master;

import java.util.Comparator;

public class DaemonAgeComparator implements Comparator<DaemonEntry> {

	public DaemonAgeComparator() {
	}

	@Override
	public int compare(DaemonEntry first, DaemonEntry that) {
			// more recent entries are lower than older entries
			if (first.getRefreshedAt() > that.getRefreshedAt())
				return -1;
			if (first.getRefreshedAt() < that.getRefreshedAt())
				return 1;
			return 0;
	}
}
