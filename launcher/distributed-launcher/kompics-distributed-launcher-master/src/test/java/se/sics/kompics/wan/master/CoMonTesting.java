package se.sics.kompics.wan.master;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;

import org.junit.Ignore;
import se.sics.kompics.wan.plab.CoMonStats;
import se.sics.kompics.wan.plab.CoMonStatsComparator;
import se.sics.kompics.wan.plab.PLabHost;

public class CoMonTesting {

	@Before
	public void setUp() throws Exception {
	}

//	@org.junit.Test
        @Ignore
	public void testSorting()
	{
		
		try {
			List<PLabHost> hosts = new ArrayList<PLabHost>();
	
			
			File f = new File("src/test/resources/comon.dat");
			BufferedReader in = new BufferedReader(new InputStreamReader(			
					new FileInputStream(f)));
			
			String titleLine = in.readLine();
			int downloadedBytes = titleLine.length();
			if (titleLine != null
					&& titleLine.split(",").length == CoMonStats.NUM_COMON_COLUMNS) {
	
				String inputLine = in.readLine();
				while ((inputLine = in.readLine()) != null) {
					CoMonStats statEntry = new CoMonStats(inputLine);
					String hostname = statEntry.getHostname();
					PLabHost plHost = new PLabHost(hostname);
					plHost.setCoMonStat(statEntry);
					hosts.add(plHost);
					downloadedBytes += inputLine.length();
				}
			} else if (titleLine.split(",").length != CoMonStats.NUM_COMON_COLUMNS){
				System.err.println("New comon format, please upgrade your plman version (colnum=" + titleLine.split(",").length + ")");
			}
	
			
			Collections.sort(hosts, new CoMonStatsComparator(CoMonStats.RESPTIME, CoMonStats.LIVESLICES));
			
			for (PLabHost h : hosts) {
				System.out.println(h.toString());
			}
			assert(true);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			assert(false);
		}
	}
}
