package se.sics.kompics.wan.plab;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.mina.util.ConcurrentHashSet;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.NullParser;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.serializer.NullSerializer;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.plab.events.GetNodesForSliceRequest;
import se.sics.kompics.wan.plab.events.GetNodesForSliceResponse;
import se.sics.kompics.wan.plab.events.GetNodesRequest;
import se.sics.kompics.wan.plab.events.GetNodesResponse;
import se.sics.kompics.wan.plab.events.InstallDaemonOnHostsRequest;
import se.sics.kompics.wan.plab.events.PlanetLabInit;
import se.sics.kompics.wan.plab.events.QueryPLabSitesRequest;
import se.sics.kompics.wan.plab.events.QueryPLabSitesResponse;
import se.sics.kompics.wan.plab.events.UpdateCoMonStats;
import se.sics.kompics.wan.plab.events.UpdateHostsAndSites;

public class PLabComponent extends ComponentDefinition {

	private Negative<PLabPort> pLabPort = negative(PLabPort.class);

	private final Logger logger = LoggerFactory.getLogger(PLabComponent.class);

	private Positive<Network> net = positive(Network.class);
	private Positive<Timer> timer = positive(Timer.class);

	private static int DNS_RESOLVER_MAX_THREADS = 10;

	private static final String COMON_URL = "http://summer.cs.princeton.edu/status/tabulator.cgi?"
			+ "table=table_nodeviewshort&format=formatcsv";

	public final static double MAX_PLC_CACHE_AGE = 24;

	// private double progress = 0.0;

	private boolean retryingWithCertIgnore = false;

	private PLabService pLabService;

	private PlanetLabCredentials cred;

	// private PLabStore store;

	private Set<PLabSite> sites = null;

//	private List<Integer> sliceNodes = null;

	Set<PLabHost> hosts = new ConcurrentHashSet<PLabHost>();

	public PLabComponent() {

		subscribe(handleGetNodesForSliceRequest, pLabPort);
		subscribe(handleQueryPLabSitesRequest, pLabPort);
		subscribe(handleInstallDaemonOnHostsRequest, pLabPort);

		subscribe(handleUpdateCoMonStats, pLabPort);
		subscribe(handleUpdateHostsAndSites, pLabPort);

		subscribe(handlePlanetLabInit, control);
	}

	public Handler<PlanetLabInit> handlePlanetLabInit = new Handler<PlanetLabInit>() {
		public void handle(PlanetLabInit event) {

			cred = event.getCred();
			
			pLabService = (PLabService) PlanetLabConfiguration.getCtx().getBean("PLabService");

			long startTime = System.currentTimeMillis();
			// System.out.println("sending PLC queries");
			System.out.println("Loading hosts from filesystem");
			// progress = 0.2;

			PLabStore store = pLabService.load(cred.getSlice());

			long time;
			if (store != null) {

				Set<PLabHost> listHosts = store.getHosts();

				hosts.addAll(listHosts);

				sites = store.getSites();

//				sliceNodes = store.getSliceNodes();

				time = System.currentTimeMillis() - startTime;
				System.out.println("Loading hosts done: " + time + "ms");
			} else {
				
				updateHostsAndSites();
			}

		}
	};

	private Handler<UpdateCoMonStats> handleUpdateCoMonStats = new Handler<UpdateCoMonStats>() {
		public void handle(UpdateCoMonStats event) {

			updateCoMonStats();
		}
	};

	private void updateCoMonStats() {
		PlCoMon plCoMon = new PlCoMon(hosts);
		new Thread(plCoMon).start();
	}

	private Handler<UpdateHostsAndSites> handleUpdateHostsAndSites = new Handler<UpdateHostsAndSites>() {
		public void handle(UpdateHostsAndSites event) {

			updateHostsAndSites();
		}
	};
	
	private void updateHostsAndSites()
	{
		long startTime = System.currentTimeMillis();
		// query for hosts
		Set<PLabHost> hosts = getNodes();
		long time = System.currentTimeMillis() - startTime;
		System.out.println("PLC host query done: " + time + "ms");

		// start the dns resolver
		ParallelDNSLookup dnsLookups = new ParallelDNSLookup(DNS_RESOLVER_MAX_THREADS,
				hosts);
		dnsLookups.performLookups();

		// progress = 0.4;
		Set<PLabSite> sites = getSites();
		time = System.currentTimeMillis() - startTime;
		System.out.println("PLC site query done: " + time + "ms");

		// progress = 0.6;
		List<Integer> sliceNodes = getNodesForSlice();
		time = System.currentTimeMillis() - startTime;
		System.out.println("PLC slice query done: " + time + "ms");

		// progress = 0.8;
//		HashMap<Integer, Integer> hostToSiteMapping = queryPLCHostToSiteMapping();
//		time = System.currentTimeMillis() - startTime;
//		System.out.println("PLC all queries done: " + time + "ms");

		System.out.println("waiting for hostnames to get resolved");

		dnsLookups.join();

		time = System.currentTimeMillis() - startTime;
		System.out.println("dns queries done: " + time + "ms");

//		progress = 0.9;
//		for (PLabHost host : hosts) {
//			host.setSiteId(hostToSiteMapping.get(host.getNodeId()));
//		}

		// XXX order hosts by number of active slices or response time
		// or
		// geography

		PLabStore downloadedStore = new PLabStore(cred.getSlice(), cred.getUsername()); // MD5
		downloadedStore.setHosts(hosts);
		downloadedStore.setSites(sites);

		System.out.println("hosts : " + hosts.size());
		System.out.println("sites : " + sites.size());
//		downloadedStore.setSliceNodes(sliceNodes);
		pLabService.save(downloadedStore);

		hosts.addAll(hosts);
		this.sites = sites;
//		this.sliceNodes = sliceNodes;
	}
	

	private Handler<InstallDaemonOnHostsRequest> handleInstallDaemonOnHostsRequest = new Handler<InstallDaemonOnHostsRequest>() {
		public void handle(InstallDaemonOnHostsRequest event) {

			for (PLabHost host : hosts) {

				// connect first, then uploadFile

				// trigger(new UploadFileRequest())

			}
		}

	};

	private Handler<GetNodesForSliceRequest> handleGetNodesForSliceRequest = new Handler<GetNodesForSliceRequest>() {
		public void handle(GetNodesForSliceRequest event) {

			List<Integer> sliceNodes = getNodesForSlice();

			GetNodesForSliceResponse respEvent = new GetNodesForSliceResponse(event, sliceNodes);

			trigger(respEvent, pLabPort);

		}
	};

	private List<Integer> getNodesForSlice() {

		List<Object> params = new ArrayList<Object>();
		params.add(cred.getPLCMap());

		HashMap<String, Object> filter = new HashMap<String, Object>();
		filter.put("name", cred.getSlice());
		params.add(filter);

		ArrayList<String> returnFields = new ArrayList<String>();
		returnFields.add("node_ids");
		params.add(returnFields);

		Object response = executeRPC("GetSlices", params);

		List<Integer> sliceNodes = new ArrayList<Integer>();
		if (response instanceof Object[]) {
			Object[] result = (Object[]) response;
			if (result.length > 0) {
				Map map = (Map) result[0];
				Object[] nodes = (Object[]) map.get("node_ids");
				for (Object node : nodes) {
					sliceNodes.add((Integer) node);
				}
			}
		}
		return sliceNodes;
	}

	private Handler<GetNodesRequest> handleGetNodesRequest = new Handler<GetNodesRequest>() {
		public void handle(GetNodesRequest event) {

			Set<PLabHost> hosts = getNodes();
			GetNodesResponse respEvent = new GetNodesResponse(event, hosts);
			trigger(respEvent, pLabPort);
		}
	};

	private Set<PLabHost> getNodes() {
		List<String> fields = new ArrayList<String>();
		fields.add(PLabHost.NODE_ID);
		fields.add(PLabHost.HOSTNAME);
		fields.add(PLabHost.BOOT_STATE);
		// fields.add(ExperimentHost.IP);
		// fields.add(ExperimentHost.BWLIMIT);
		fields.add(PLabSite.SITE_ID);

		List<Object> params = new ArrayList<Object>();
		params.add(cred.getPLCMap());
		// plc api change, an empty map will return all nodes
		params.add(new HashMap());
		params.add(fields);

		Set<PLabHost> hosts = new HashSet<PLabHost>();
		Object response = executeRPC("GetNodes", params);
		if (response instanceof Object[]) {
			Object[] result = (Object[]) response;
			for (Object obj : result) {
				Map map = (Map) obj;
				PLabHost host = new PLabHost(map);
				hosts.add(host);
			}
		} else {
			logger.warn("Nothing returned by GetNodes");
		}
		return hosts;
	}

	// public Handler<UpdateCoMonStats> handleGetBootStates = new
	// Handler<UpdateCoMonStats>() {
	// @SuppressWarnings("unchecked")
	// public void handle(UpdateCoMonStats event) {
	//			
	// Vector params = new Vector();
	// params.add(cred.getAuthMap());
	// executeRPC("GetBootStates", params);
	//			
	// }
	// };

	private Handler<QueryPLabSitesRequest> handleQueryPLabSitesRequest = new Handler<QueryPLabSitesRequest>() {
		public void handle(QueryPLabSitesRequest event) {

			Set<PLabSite> sites = getSites();

			QueryPLabSitesResponse respEvent = new QueryPLabSitesResponse(event, sites);

			trigger(respEvent, pLabPort);
		}
	};

	private Set<PLabSite> getSites() {
		List<Object> rpcParams = new ArrayList<Object>();
		rpcParams.add(cred.getPLCMap());

		HashMap<String, Object> filter = new HashMap<String, Object>();
		rpcParams.add(filter);

		ArrayList<String> returnValues = new ArrayList<String>();
		returnValues.add(PLabSite.NAME);
		returnValues.add(PLabSite.ABBREVIATED_NAME);
		returnValues.add(PLabSite.SITE_ID);
		returnValues.add(PLabSite.LOGIN_BASE);
		rpcParams.add(returnValues);

		Set<PLabSite> sites = new HashSet<PLabSite>();

		Object response = executeRPC("GetSites", rpcParams);
		if (response instanceof Object[]) {
			Object[] result = (Object[]) response;
			for (Object res : result) {
				Map map = (Map) res;
				PLabSite site = new PLabSite(map);
				sites.add(site);
			}
		}
		return sites;
	}

	private Object executeRPC(String command, List<Object> params) {
		Object res = new Object();
		String url = PlanetLabConfiguration.getPlcApiAddress();

		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		try {
			// System.out.println("using plc api at: " + url);
			config.setServerURL(new URL(url));
			config.setEncoding("iso-8859-1");
			config.setEnabledForExtensions(true);

			XmlRpcClient client = new XmlRpcClient();
			client.setTypeFactory(new MyTypeFactory(client));

			client.setConfig(config);

			res = (Object) client.execute(command, params);
		} catch (XmlRpcException e) {
			if (e.getMessage().contains("java.security.cert.CertPathValidatorException")) {
				System.err.println("There is a problem with the PlanetLab Central certificate: ");
				System.err.println("Config says: IgnoreCertificateErrors="
						+ cred.isIgnoreCerificateErrors());
				if (cred.isIgnoreCerificateErrors() && retryingWithCertIgnore == false) {

					System.out.println("trying again");

					this.ignoreCertificateErrors();
					return this.executeRPC(command, params);
				} else {
					System.err
							.println("You need to add IgnoreCertificateErrors=true to the config file to have PLC XML-RPC support");
					System.err.println("XML-RPC error: " + e.getMessage());
				}
			} else {
				System.err.println(e.getMessage());
				// e.printStackTrace();
			}
		} catch (MalformedURLException e) {
			System.err.println("There was an error when trying to connect to: " + url);
			System.err.println("Did you specify it correctly in the config file?");
		}

		return res;
	}

	private void ignoreCertificateErrors() {
		retryingWithCertIgnore = true;
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
				// Trust always
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
				// Trust always
			}
		} };

		// Install the all-trusting trust manager
		SSLContext sc;
		try {
			sc = SSLContext.getInstance("SSL");

			// Create empty HostnameVerifier
			HostnameVerifier hv = new HostnameVerifier() {
				public boolean verify(String arg0, SSLSession arg1) {
					return true;
				}
			};

			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(hv);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private class MyTypeFactory extends TypeFactoryImpl {
		public MyTypeFactory(XmlRpcController pController) {
			super(pController);
		}

		public TypeParser getParser(XmlRpcStreamConfig pConfig, NamespaceContextImpl pContext,
				String pURI, String pLocalName) {

			// the plc api is using nil, but not reporting that they are using
			// extensions
			if (NullSerializer.NIL_TAG.equals(pLocalName)) {
				return new NullParser();
			}
			// System.err.println("pURI: " + pURI + " plocalName: " +
			// pLocalName);

			return super.getParser(pConfig, pContext, pURI, pLocalName);

		}

		public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig, Object pObject)
				throws SAXException {
			return super.getSerializer(pConfig, pObject);
		}
	}

	private class ParallelDNSLookup {
		private final ExecutorService threadPool;

		private final Set<PLabHost> hosts;

		private final ConcurrentLinkedQueue<Future<String>> tasks;

		public ParallelDNSLookup(int numThreads, Set<PLabHost> hosts) {
			this.hosts = new HashSet<PLabHost>(hosts);

			this.threadPool = Executors.newFixedThreadPool(numThreads);
			this.tasks = new ConcurrentLinkedQueue<Future<String>>();
		}

		public void performLookups() {
			int i = 0;
			for (PLabHost host : hosts) {
				Future<String> task = threadPool.submit(new ResolveIPHandler(host, i++));
				tasks.add(task);
			}
		}

		public void join() {
			Future<String> task;
			try {
				// for all tasks
				while ((task = tasks.poll()) != null) {
					// block until task is done
					String result = (String) task.get();
					if (!result.equals("")) {
						System.err.println(result);
					}
				}
				threadPool.shutdown();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	private class ResolveIPHandler implements Callable<String> {
		private PLabHost host;

		int num;

		public ResolveIPHandler(PLabHost host, int num) {
			this.host = host;
			this.num = num;
		}

		public String call() {

			try {
				InetAddress addr = InetAddress.getByName(host.getHostname());
				host.setIp(addr.getHostAddress());
				// System.out.println(num + ":" + host.getIp());
				return "";
				// System.out.println(host.getHostname());
			} catch (UnknownHostException e) {
				host.setIp(null);
				return ("unknown host: " + host.getHostname());
			}
		}

	}

	public class PlCoMon implements Runnable {

		private static final String COMON_URL = "http://summer.cs.princeton.edu/status/tabulator.cgi?"
				+ "table=table_nodeviewshort&format=formatcsv";

		private int totalBytes = 0;

		private volatile double progress = 0;

		private HashMap<String, PLabHost> plHostMap;

		public PlCoMon(Set<PLabHost> hosts) {
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

				in = new BufferedReader(new InputStreamReader(url.openStream()));

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
							System.out.println(plHost.getComMonStat().getHostname() + ": "
									+ plHost.getComMonStat().getStat(CoMonStats.RESPTIME));
						}

						downloadedBytes += inputLine.length();

						progress = ((double) downloadedBytes) / (double) totalBytes;
						// System.out.println(progress);
					}
				} else if (titleLine.split(",").length != CoMonStats.NUM_COMON_COLUMNS) {
					System.err
							.println("New comon format, please upgrade your plman version (colnum="
									+ titleLine.split(",").length + ")");
				}

			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
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

	}

	/**
	 * @param hosts
	 * @param coMonStat
	 *            One of the parameters in CoMonStats.STATS
	 * @return
	 */
	public static List<PLabHost> orderBy(List<PLabHost> hosts, String coMonStat) {
		Collections.sort(hosts, new CoMonStats.CoMonComparator(coMonStat));

		return hosts;
	}
}
