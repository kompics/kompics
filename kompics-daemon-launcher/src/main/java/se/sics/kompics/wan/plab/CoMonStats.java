package se.sics.kompics.wan.plab;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CoMonStats {

	public static final String NAME = "name";
	public static final String ADDRESS = "address";
	public static final String LOCATION = "location";
	public static final String RESPTIME = "resptime";
	public static final String SSH_STATUS = "sshstatus";
	public static final String UPTIME = "uptime";
	public static final String LAST_COTOP = "lastcotop";
	public static final String NODE_TYPE = "nodetype";
	public static final String BOOT_STATE = "bootstate";
	public static final String KEYOK = "keyok";
	public static final String KERNVER = "kernver";
	public static final String FCNAME = "fcname";
	public static final String DATE = "date";
	public static final String DRIFT = "drift";
	public static final String CPUSPEED = "cpuspeed";
	public static final String BUSYCPU = "busycpu";
	public static final String SYSCPU = "syscpu";
	public static final String FREECPU = "freecpu";
	public static final String ONE_MINLOAD = "1minload";
	public static final String FIVE_MINLOAD = "5minload";
	public static final String NUMSLICES = "numslices";
	public static final String LIVESLICES = "liveslices";
	public static final String TIMERMAX = "timermax";
	public static final String TIMERAVG = "timeravg";
	public static final String CONNMAX = "connmax";
	public static final String CONNAVG = "connavg";
	public static final String MEMSIZE = "memsize";
	public static final String MEMACT = "memact";
	public static final String FREEMEM = "freemem";
	public static final String SWAPIN = "swapin";
	public static final String SWAPOUT = "swapout";
	public static final String DISKIN = "diskin";
	public static final String DISKOUT = "diskout";
	public static final String DISKUTIL = "diskutil";
	public static final String IOSTATAWAIT = "iostatawait";
	public static final String IOSTATSVC = "iostatsvc";
	public static final String DISKSIZE = "disksize";
	public static final String DISKUSED = "diskused";
	public static final String GBFREE = "gbfree";
	public static final String SWAPUSED = "swapused";
	public static final String FDTEST = "fdtest";
	public static final String FILERW = "filerw";
	public static final String BWLIMIT = "bwlimit";
	public static final String TXRATE = "txrate";
	public static final String RXRATE = "rxrate";
	public static final String DNS1UDP = "dns1udp";
	public static final String DNS1TCP = "dns1tcp";
	public static final String DNS2UDP = "dns2udp";
	public static final String DNS2TCP = "dns2tcp";
	public static final String RAWPORTS = "rawports";
	public static final String ICMPPORTS = "icmpports";
	public static final String LONGPORTS = "longports";
	public static final String SNAPPORTS = "snapports";
	public static final String NON206 = "non206";
	public static final String HASVIA = "hasvia";

	public static final String[] STATS = { NAME, ADDRESS, LOCATION, RESPTIME, SSH_STATUS, UPTIME,
			LAST_COTOP, NODE_TYPE, BOOT_STATE, KEYOK, KERNVER, FCNAME, DATE, DRIFT, CPUSPEED,
			BUSYCPU, SYSCPU, FREECPU, ONE_MINLOAD, FIVE_MINLOAD, NUMSLICES, LIVESLICES, TIMERMAX,
			TIMERAVG, CONNMAX, CONNAVG, MEMSIZE, MEMACT, FREEMEM, SWAPIN, SWAPOUT, DISKIN, DISKOUT,
			DISKUTIL, IOSTATAWAIT, IOSTATSVC, DISKSIZE, DISKUSED, GBFREE, SWAPUSED, FDTEST, FILERW,
			BWLIMIT, TXRATE, RXRATE, DNS1UDP, DNS1TCP, DNS2UDP, DNS2TCP, RAWPORTS, ICMPPORTS,
			LONGPORTS, SNAPPORTS, NON206, HASVIA };
	
	public static final int NUM_COMON_COLUMNS = CoMonStats.STATS.length;

	
	static class CoMonComparator implements Comparator<PLabHost> {

		private String stat;
		public CoMonComparator(String stat) {
			if (containsStat(stat) == false) {
				throw new IllegalArgumentException("Invalid statName. Check CoMonStat.STATS for valid stat names. Name was: " + stat);
			}
			this.stat = stat;
		}
		@Override
		public int compare(PLabHost first, PLabHost that) {
				if (first.getComMonStat().getStat(stat) > 
					that.getComMonStat().getStat(stat))
					return -1;
				if (first.getComMonStat().getStat(stat) < 
						that.getComMonStat().getStat(stat))
					return 1;
				return 0;
		}
	}
	
	/********************************************************************/
	// Member variables
	/********************************************************************/
		
	private String hostname;

	private Map<String, Double> mapStats = new ConcurrentHashMap<String, Double>();

	public CoMonStats() {

	}

	public CoMonStats(String line) {
		String[] row = line.split(",");

		if (row.length != CoMonStats.STATS.length) {
			System.err.println("Invalid number of columns returned by CoMon: " + row.length);
		}
		processRow(row);
	}

	private void processRow(String[] row) {
		hostname = row[0];

		for (int i = 1; i < row.length; i++) {
			Double val;
			row[i] = row[i].trim();
			if (row[i].compareTo("NULL") == 0 || row[i].compareTo("null") == 0 || row[i] == null) {
				val = Double.NaN;
			} else {
				val = Double.parseDouble(row[i]);
			}
			mapStats.put(STATS[i], val);
		}

	}

	public String getHostname() {
		return hostname;
	}

	public Double getStat(String statName) {
		
		if (containsStat(statName) == false)
		{
			throw new IllegalArgumentException("Invalid statName. Check CoMonStat.STATS for valid stat names. Name was: " + statName);
		}
		
		return mapStats.get(statName);
	}
	
	private static boolean containsStat(String statName)
	{
		boolean found = false;
		for (int i=1; i<STATS.length; i++)
		{
			if (STATS[i].compareTo(statName) == 0)
			{
				found = true;
			}
		}
		return found;
	}
	
	@Override
	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		for (String s : mapStats.keySet())
		{
			buf.append("(" + s + ",");
			buf.append(mapStats.get(s) + ") " );
		}
		return buf.toString();
	}
	
}
