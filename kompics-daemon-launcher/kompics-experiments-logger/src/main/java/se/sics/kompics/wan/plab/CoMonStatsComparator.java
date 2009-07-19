package se.sics.kompics.wan.plab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * The <code>ConnectionComparator</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class CoMonStatsComparator implements Comparator<PLabHost> {

	private final List<String> stats;
	
	public CoMonStatsComparator(String... stats)
	{
		this.stats = new ArrayList<String>(Arrays.asList(stats));
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(PLabHost arg0, PLabHost arg1) {
		
		CoMonStats c0 = arg0.getComMonStat();
		CoMonStats c1 = arg1.getComMonStat();

		if (c0 == null && c1 == null) {
			return 0;
		}
		else if (c1 == null) {
			return 1;
		}
		else if (c0 == null) {
			return -1;
		}

		return compareStats(c0, c1);
	}
	
	private int compareStats(CoMonStats c0, CoMonStats c1){
		
		for (String stat : stats) {
		
			if (c0.getStat(stat) == c1.getStat(stat)) {
				continue;
			}
			else if (c0.getStat(stat) < c1.getStat(stat)) {
				return -1;
			}
			else if (c0.getStat(stat) > c1.getStat(stat)) {
				return 1;
			}
		}
		return 0;
	}
	
}
