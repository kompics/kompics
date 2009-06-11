package se.sics.kompics.wan.master.plab.plc.comon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import se.sics.kompics.wan.master.plab.plc.PlanetLabHost;


public class CoMonManager implements Runnable {

	// private hosts;

	private int totalBytes = 0;

	private volatile double progress = 0;

	private static final String COMON_URL = "http://summer.cs.princeton.edu/status/tabulator.cgi?"
			+ "table=table_nodeviewshort&format=formatcsv";

	public static final int COMON_NUM_FIELDS = 52;

	private HashMap<String, PlanetLabHost> plHostMap;

	public static void main(String[] args) {
		Thread t = new Thread(new CoMonManager(new PlanetLabHost[0]));
		t.start();
	}

	public CoMonManager(PlanetLabHost[] hosts) {
		plHostMap = new HashMap<String, PlanetLabHost>();

		for (int i = 0; i < hosts.length; i++) {
			plHostMap.put(hosts[i].getHostname(), hosts[i]);
		}
	}

	public void run() {
		// TODO Auto-generated method stub
		totalBytes = 0;
		progress = 0;
		try {
			URL url = new URL(COMON_URL);

			// HttpURLConnection connection = (HttpURLConnection) url
			// .openConnection();

			// ok, this IS an uggly hack :-)
			totalBytes = 240717;// connection.getContentLength();

			BufferedReader in = new BufferedReader(new InputStreamReader(url
					.openStream()));

			String titleLine = in.readLine();
			int downloadedBytes = titleLine.length();
			if (titleLine != null
					&& titleLine.split(",").length == COMON_NUM_FIELDS) {

				String inputLine = in.readLine();
				while ((inputLine = in.readLine()) != null) {
					CoMonStat statEntry = new CoMonStat(inputLine);
					String hostname = statEntry.getHostname();
					if (plHostMap.containsKey(hostname)) {
						PlanetLabHost plHost = plHostMap.get(hostname);
						plHost.setCoMonStat(statEntry);
						// System.out.println(plHost.getComMonStat().getHostname()
						// + ": "
						//								+ plHost.getComMonStat().getResponseTime());
					}

					downloadedBytes += inputLine.length();

					progress = ((double) downloadedBytes) / (double) totalBytes;
					// System.out.println(progress);
				}
			} else if (titleLine.split(",").length != COMON_NUM_FIELDS){
				System.err.println("New comon format, please upgrade your plman version (colnum=" + titleLine.split(",").length + ")");
			}

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		progress = 1.0;
	}

	public double getProgress() {
		return progress;
	}
}
