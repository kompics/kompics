package se.sics.kompics.wan.plab;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;



public class CoMon implements Runnable {

	private int totalBytes = 0;

	private volatile double progress = 0;

	private static final String COMON_URL = "http://summer.cs.princeton.edu/status/tabulator.cgi?"
			+ "table=table_nodeviewshort&format=formatcsv";

	private HashMap<String, PLabHost> plHostMap;

	public CoMon(List<PLabHost> hosts) {
		plHostMap = new HashMap<String, PLabHost>();

		for (PLabHost host : hosts) {
			plHostMap.put(host.getHostname(), host);
		}
	}

	public void run() {
		// TODO Auto-generated method stub
		totalBytes = 0;
		progress = 0;
		BufferedReader in = null;
		try {
			URL url = new URL(COMON_URL);

			// HttpURLConnection connection = (HttpURLConnection) url
			// .openConnection();

			// ok, this IS an uggly hack :-)
			totalBytes = 240717;// connection.getContentLength();

			in = new BufferedReader(new InputStreamReader(url
					.openStream()));

			String titleLine = in.readLine();
			int downloadedBytes = titleLine.length();
			if (titleLine != null
					&& titleLine.split(",").length == CoMonStats.NUM_COMON_COLUMNS) {

				String inputLine = in.readLine();
				while ((inputLine = in.readLine()) != null) {
					CoMonStats statEntry = new CoMonStats(inputLine);
					String hostname = statEntry.getHostname();
					if (plHostMap.containsKey(hostname)) {
						PLabHost plHost = plHostMap.get(hostname);
						plHost.setCoMonStat(statEntry);
						 System.out.println(plHost.getComMonStat().getHostname()
						 + ": "	+ plHost.getComMonStat().getStat(CoMonStats.RESPTIME));
					}

					downloadedBytes += inputLine.length();

					progress = ((double) downloadedBytes) / (double) totalBytes;
					// System.out.println(progress);
				}
			} else if (titleLine.split(",").length != CoMonStats.NUM_COMON_COLUMNS){
				System.err.println("New comon format, please upgrade your plman version (colnum=" + titleLine.split(",").length + ")");
			}

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					System.err.println("Problem closing connection to CoMonManager URL");
				}
			}
		}
		progress = 1.0;
	}

	public double getProgress() {
		return progress;
	}
	
	/**
	 * @param hosts
	 * @param coMonStat One of the parameters in CoMonStats.STATS
	 * @return
	 */
	public static List<PLabHost> orderBy(List<PLabHost> hosts, String coMonStat)
	{
		Collections.sort(hosts, new CoMonStats.CoMonComparator(coMonStat));
		
		return hosts;
	}
}
