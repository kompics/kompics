package se.sics.kompics.wan.master.plab.plc;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
import org.xml.sax.SAXException;

import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.master.plab.Credentials;


import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class PLCentralCache implements Runnable {

	private boolean retryingWithCertIgnore = false;

	private static int DNS_RESOLVER_MAX_THREADS = 10;

	private static final String PLC_DISK_CACHE_FILE = "plc_disk_cache.xml.gz";

	public final static double MAX_PLC_CACHE_AGE = 24;

	private final Credentials cred;

	private ConcurrentHashMap<PlanetLabHost, PlanetLabSite> hostToSiteMapping;

	private ConcurrentHashMap<Integer, PlanetLabHost> hostIdToHostMapping;

	private final Object mutex = new Object();

	// private ConcurrentHashMap<Integer, PlanetLabHost> plcHostIdCache = new
	// ConcurrentHashMap<Integer, PlanetLabHost>();

	private volatile double progress = 0;

	private ConcurrentHashMap<Integer, PlanetLabSite> siteIdToSiteMapping;

	private ConcurrentHashMap<PlanetLabSite, Vector<PlanetLabHost>> sitesToHostMapping;

	private PlanetLabStore store;

	public PLCentralCache(Credentials cred) {
		// this.ignoreCertificateErrors();
		this.cred = cred;
		Thread t = new Thread(this);
		t.setDaemon(true);
		t.setName("PLC cache");
		t.start();
	}

	public int addHostsToSlice(PlanetLabHost[] hosts) {

		// using only public functions, no need to synch
		// only add hosts not already in the slice
		PlanetLabHost[] newHosts = this.getHostsNotInSlice(hosts);
		if (newHosts.length > 0) {

			Vector<String> nodesToAdd = new Vector<String>();

			for (int i = 0; i < newHosts.length; i++) {
				// for (int i = 0; i < newHosts.length; i++) {
				nodesToAdd.add(newHosts[i].getHostname());
			}
			Vector<Object> params = new Vector<Object>();

			params.add(cred.getPLCMap());
			params.add(cred.getSlice());
			params.add(nodesToAdd);
			this.executeRPC("SliceNodesAdd", params);
			System.out.println("added " + nodesToAdd.size() + " new hosts");

			// remove old disk cache and query for a new one
			this.removeDiskCache();
			Thread t = new Thread(this);
			t.start();

		}

		return newHosts.length;

	}

	private void ignoreCertificateErrors() {
		retryingWithCertIgnore = true;
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs,
					String authType) {
				// Trust always
			}

			public void checkServerTrusted(X509Certificate[] certs,
					String authType) {
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
			HttpsURLConnection
					.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(hv);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void debugGetMethodHelp(String method) {
		Vector<Object> params = new Vector<Object>();
		params.add(method);
		System.out.println("method help:");
		this.executeRPC("methodHelp(method)", params);
		System.out.println("\nmethod signature:");
		this.executeRPC("methodSignature", params);
	}

	public void debugListMethods() {
		Vector<Object> params = new Vector<Object>();
		this.executeRPC("listMethods()", params);
	}

	public PlanetLabHost[] getAllHosts() {
		synchronized (mutex) {
			return store.getHosts();
		}
	}

	public PlanetLabHost[] getHostsNotInSlice(PlanetLabHost[] hosts) {
		ArrayList<PlanetLabHost> newHosts = new ArrayList<PlanetLabHost>();
		synchronized (mutex) {
			for (PlanetLabHost host : hosts) {
				if (!hostToSiteMapping.containsKey(host)) {
					newHosts.add(host);
					// System.out.println("adding: " + host.getHostname());
				}
			}
		}

		PlanetLabHost[] newArray = new PlanetLabHost[newHosts.size()];
		newArray = newHosts.toArray(newArray);
		return newArray;
	}

	public ConcurrentHashMap<PlanetLabHost, PlanetLabSite> getHostToSiteMapping() {
		synchronized (mutex) {
			return hostToSiteMapping;
		}

	}

	public double getProgress() {
		return progress;
	}

	public PlanetLabSite getSite(int siteId) {
		return siteIdToSiteMapping.get(siteId);
	}

	public PlanetLabHost getHost(int plcHostId) {
		synchronized (mutex) {
			return hostIdToHostMapping.get(plcHostId);
		}
	}

	public ConcurrentHashMap<PlanetLabSite, Vector<PlanetLabHost>> getSiteToHostMapping() {
		synchronized (mutex) {
			return sitesToHostMapping;
		}
	}

	public void run() {
		progress = 0;
		store = null;
		synchronized (mutex) {

			// check if we have a valid copy on disk
			store = this.hostStoreReadFromDisk();

			// if not, send quries
			if (store == null) {
				store = sendPLCQueries();
			}
			this.hostStoreWriteToDisk(store);
			this.createHashMaps(store);
		}
		// write the store to disk

		System.out.println("" + hostToSiteMapping.size() + " of "
				+ store.getHosts().length + " hosts in slice");
		progress = 1;
	}

	private void createHashMaps(PlanetLabStore store) {
		hostToSiteMapping = new ConcurrentHashMap<PlanetLabHost, PlanetLabSite>();
		sitesToHostMapping = new ConcurrentHashMap<PlanetLabSite, Vector<PlanetLabHost>>();
		siteIdToSiteMapping = new ConcurrentHashMap<Integer, PlanetLabSite>();
		hostIdToHostMapping = new ConcurrentHashMap<Integer, PlanetLabHost>();
		// first. create a hashmap of all hostnames in the slice
		int[] sliceNodes = store.getSliceNodes();
		// System.out.println("slice_nodes: " + sliceNodes.length);
		HashMap<Integer, Boolean> sliceHosts = new HashMap<Integer, Boolean>(
				sliceNodes.length * 2);
		for (int i = 0; i < sliceNodes.length; i++) {
			int hostId = sliceNodes[i];
			sliceHosts.put(hostId, true);

		}

		// second, create a siteID to site mapping
		PlanetLabSite[] sites = store.getSites();
		// System.out.println("sites : " + sites.length);
		siteIdToSiteMapping = new ConcurrentHashMap<Integer, PlanetLabSite>(
				sites.length * 2);
		for (PlanetLabSite site : sites) {
			// System.out.println(site.getName() + ":" + site.getSite_id());
			siteIdToSiteMapping.put(site.getSite_id(), site);
		}

		// then, for all hosts:
		PlanetLabHost[] hosts = store.getHosts();
		// System.out.println("hosts : " + hosts.length);
		for (PlanetLabHost host : hosts) {
			// System.out.println(host.getHostname());
			PlanetLabSite site = siteIdToSiteMapping.get(host.getSite());
			hostIdToHostMapping.put(host.getNode_id(), host);
			// check if the host is in the slice
			if (sliceHosts.containsKey(host.getNode_id())) {
				// always add to host to site mapping
				this.hostToSiteMapping.put(host, site);

				// add to site to host mapping
				if (!this.sitesToHostMapping.containsKey(site)) {
					this.sitesToHostMapping.put(site,
							new Vector<PlanetLabHost>());

				}
				this.sitesToHostMapping.get(site).add(host);
			}
		}
	}

	private Object executeRPC(String command, Vector<Object> params) {
		Object res = new Object();
		String url = PlanetLabConfiguration.getPlcApiAddress(); 
			//Main.getConfig(Constants.PLC_API_ADDRESS);
		if(url== null){
			url = "https://www.planet-lab.org/PLCAPI/";
		}
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		try {
			//System.out.println("using plc api at: " + url);
			config.setServerURL(new URL(url));
			config.setEncoding("iso-8859-1");
			config.setEnabledForExtensions(true);

			XmlRpcClient client = new XmlRpcClient();
			client.setTypeFactory(new MyTypeFactory(client));

			client.setConfig(config);

			res = (Object) client.execute(command, params);
		} catch (XmlRpcException e) {
			// TODO Auto-generated catch block
			if (e.getMessage().contains(
					"java.security.cert.CertPathValidatorException")) {
				System.err
						.println("There is a problem with the PlanetLab Central certificate: ");
				System.err.println("Config says: IgnoreCertificateErrors="
						+ cred.isIgnoreCerificateErrors());
				if (cred.isIgnoreCerificateErrors()
						&& retryingWithCertIgnore == false) {

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

	private PlanetLabStore hostStoreReadFromDisk() {
		// Deserialize an object
		try {
			XMLDecoder decoder = new XMLDecoder(new GZIPInputStream(
					new BufferedInputStream(new FileInputStream(
							PLC_DISK_CACHE_FILE))));

			PlanetLabStore store = (PlanetLabStore) decoder.readObject();
			decoder.close();
			if (store != null) {
				Date currentTime = new Date();
				double storeAge = (currentTime.getTime() - store
						.getCreationTime().getTime())
						/ (60.0 * 60.0 * 1000);
				int hostNum = store.getHosts().length;

				if (!cred.getSlice().equals(store.getSlice())) {
					System.err
							.println("Plc disk cache created with different slice");
					return null;
				}
				if (!cred.getUsernameMD5().equals(store.getUsername())) {
					System.err
							.println("Plc disk cache created with different username");

				}

				if (storeAge < MAX_PLC_CACHE_AGE && hostNum > 0) {
					System.out.println("Using plc disk cache, (age="
							+ (Math.round(storeAge * 100) / 100.0) + "h)");
					return store;
				} else {
					System.out.println("PLC disk cache to old (age="
							+ (Math.round(storeAge * 100) / 100.0) + "h)");
				}

			} else {
				System.err.println("disk cache store == null");
			}

		} catch (FileNotFoundException e) {
			System.out.println("Disk cache not found");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private void hostStoreWriteToDisk(PlanetLabStore store) {
		try {
			// Serialize object into XML
			XMLEncoder encoder = new XMLEncoder(new GZIPOutputStream(
					new BufferedOutputStream(new FileOutputStream(
							PLC_DISK_CACHE_FILE))));
			encoder.writeObject(store);
			encoder.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void removeDiskCache() {
		File diskCache = new File(PLC_DISK_CACHE_FILE);
		diskCache.delete();

	}

	private PlanetLabHost[] queryPLCHosts() {
		// this.debugListMethods();
		// this.debugGetMethodHelp("GetNodes");

		Vector<String> fields = new Vector<String>();
		fields.add("node_id");
		fields.add("hostname");
		fields.add("boot_state");
		fields.add("ip");
		fields.add("bwlimit");
		fields.add("site_id");

		// fields.add("model");
		// fields.add("version");
		// fields.add("ssh_rsa_key");
		// fields.add("session"); //session requires admin priv
		// fields.add("nodenetwork_id");
		// fields.add("method");
		// fields.add("type");
		// fields.add("mac");
		// fields.add("gateway");
		// fields.add("network");
		// fields.add("broadcast");
		// fields.add("netmask");
		// fields.add("dns1");
		// fields.add("dns2");
		Vector<Object> params = new Vector<Object>();
		params.add(cred.getPLCMap());
		// plc api change, an empty map will return all nodes
		params.add(new HashMap());
		params.add(fields);
		PlanetLabHost[] hosts;
		Object response = this.executeRPC("GetNodes", params);
		if (!(response instanceof Object[])) {
			// there was an error, return empty set
			hosts = new PlanetLabHost[0];
		} else {
			Object[] result = (Object[]) response;
			hosts = new PlanetLabHost[result.length];
			for (int i = 0; i < result.length; i++) {
				Map map = (Map) result[i];
				PlanetLabHost host = new PlanetLabHost(map);
				hosts[i] = host;
				// System.out.println(host.getHostname());
			}
		}

		return hosts;
	}

	// private HashMap<Integer, Integer> queryPLCHostToSiteMapping() {
	// HashMap<Integer, Integer> mapping = new HashMap<Integer, Integer>(1000);
	//
	// // send xml-rpc query
	// Vector<Object> params = new Vector<Object>();
	// params.add(cred.getPLCMap());
	// HashMap result = (HashMap) this.executeRPC("AdmGetSiteNodes", params);
	//
	// // loop through all site ids
	// for (Iterator iter = result.keySet().iterator(); iter.hasNext();) {
	// String siteIdString = (String) iter.next();
	// Integer siteId = Integer.parseInt(siteIdString);
	//
	// // add all nodes as PlanetLabHosts
	// Object[] nodes = (Object[]) result.get(siteIdString);
	// for (int i = 0; i < nodes.length; i++) {
	// mapping.put((Integer) nodes[i], siteId);
	// // System.out.println("" + siteId + ":" + ((Integer) nodes[i]));
	//
	// }
	// }
	// return mapping;
	// }

	private PlanetLabSite[] queryPLCSites() {
		Vector<Object> params = new Vector<Object>();
		params.add(cred.getPLCMap());

		HashMap<String, Object> filter = new HashMap<String, Object>();
		params.add(filter);

		ArrayList<String> returnFields = new ArrayList<String>();
		returnFields.add("site_id");
		returnFields.add("name");
		returnFields.add("abbreviated_name");
		returnFields.add("latitude");
		returnFields.add("longitude");
		params.add(returnFields);

		PlanetLabSite[] sites;

		Object response = this.executeRPC("GetSites", params);
		if (!(response instanceof Object[])) {
			// there was an error, return empty set
			sites = new PlanetLabSite[0];
		} else {
			Object[] result = (Object[]) response;
			sites = new PlanetLabSite[result.length];

			for (int i = 0; i < result.length; i++) {
				Map map = (Map) result[i];
				// System.out.println("Parsing site" +
				// map.get("abbreviated_name"));
				PlanetLabSite site = new PlanetLabSite(map);
				sites[i] = site;

			}
		}
		return sites;
	}

	private int[] queryPLCSliceNodes() {

		Vector<Object> params = new Vector<Object>();
		params.add(cred.getPLCMap());

		HashMap<String, Object> filter = new HashMap<String, Object>();
		filter.put("name", cred.getSlice());
		params.add(filter);

		ArrayList<String> returnFields = new ArrayList<String>();
		returnFields.add("node_ids");
		params.add(returnFields);

		Object response = this.executeRPC("GetSlices", params);

		if (response instanceof Object[]) {
			Object[] result = (Object[]) response;
			if (result.length > 0) {
				Map map = (Map) result[0];
				Object[] nodes = (Object[]) map.get("node_ids");
				int[] sliceNodes = new int[nodes.length];
				for (int i = 0; i < nodes.length; i++) {
					sliceNodes[i] = (Integer) nodes[i];
					// System.out.println(nodes[i]);
				}
				return sliceNodes;
			}
		}
		return new int[0];
	}

	/**
	 * send all RPC queris to plc to create the disk store
	 * 
	 * @return
	 */
	private PlanetLabStore sendPLCQueries() {
		long startTime = System.currentTimeMillis();
		System.out.println("sending PLC queries");
		progress = 0.2;

		// query for hosts
		PlanetLabHost[] hosts = this.queryPLCHosts();
		long time = System.currentTimeMillis() - startTime;
		System.out.println("PLC host query done: " + time + "ms");

		// start the dns resolver

		ParallelDNSLookup dnsLookups = new ParallelDNSLookup(
				DNS_RESOLVER_MAX_THREADS, hosts);
		dnsLookups.performLookups();

		progress = 0.4;
		PlanetLabSite[] sites = this.queryPLCSites();
		time = System.currentTimeMillis() - startTime;
		System.out.println("PLC site query done: " + time + "ms");

		progress = 0.6;
		int[] sliceNodes = this.queryPLCSliceNodes();
		time = System.currentTimeMillis() - startTime;
		System.out.println("PLC slice query done: " + time + "ms");

		progress = 0.8;
		// HashMap<Integer, Integer> hostToSiteMapping = this
		// .queryPLCHostToSiteMapping();
		// time = System.currentTimeMillis() - startTime;
		System.out.println("PLC all queries done: " + time + "ms");

		System.out.println("waiting for hostnames to get resolved");

		dnsLookups.join();

		time = System.currentTimeMillis() - startTime;
		System.out.println("dns queries done: " + time + "ms");

		progress = 0.9;
		// for (int i = 0; i < hosts.length; i++) {
		// PlanetLabHost host = hosts[i];
		// host.setSite(hostToSiteMapping.get(host.getNode_id()));
		// }

		PlanetLabStore diskStore = new PlanetLabStore(cred.getSlice(), cred
				.getUsernameMD5());
		diskStore.setHosts(hosts);
		diskStore.setSites(sites);
		System.out.println("hosts : " + hosts.length);
		System.out.println("sites : " + sites.length);
		diskStore.setSliceNodes(sliceNodes);
		return diskStore;
	}

	private class MyTypeFactory extends TypeFactoryImpl {
		public MyTypeFactory(XmlRpcController pController) {
			super(pController);
		}

		public TypeParser getParser(XmlRpcStreamConfig pConfig,
				NamespaceContextImpl pContext, String pURI, String pLocalName) {

			// the plc api is using nil, but not reporting that they are using
			// extensions
			if (NullSerializer.NIL_TAG.equals(pLocalName)) {
				return new NullParser();
			}
			// System.err.println("pURI: " + pURI + " plocalName: " +
			// pLocalName);

			return super.getParser(pConfig, pContext, pURI, pLocalName);

		}

		public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig,
				Object pObject) throws SAXException {
			return super.getSerializer(pConfig, pObject);
		}
	}

	private class ParallelDNSLookup {
		private final ExecutorService threadPool;

		private final PlanetLabHost[] hosts;

		private final ConcurrentLinkedQueue<Future<String>> tasks;

		public ParallelDNSLookup(int numThreads, PlanetLabHost[] hosts) {
			this.threadPool = Executors.newFixedThreadPool(numThreads);
			this.tasks = new ConcurrentLinkedQueue<Future<String>>();
			this.hosts = hosts;
		}

		public void performLookups() {
			for (int i = 0; i < hosts.length; i++) {
				PlanetLabHost host = hosts[i];
				Future<String> task = threadPool.submit(new ResolveIPHandler(
						host, i));
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
		private PlanetLabHost host;

		int num;

		public ResolveIPHandler(PlanetLabHost host, int num) {
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

}
