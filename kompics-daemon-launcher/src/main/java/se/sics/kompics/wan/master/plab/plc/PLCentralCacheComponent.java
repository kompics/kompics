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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.master.plab.PlanetLabCredentials;
import se.sics.kompics.wan.master.plab.events.GetRunningPlanetLabHostsRequest;

public class PLCentralCacheComponent extends ComponentDefinition {

	
	private static int DNS_RESOLVER_MAX_THREADS = 10;

	private static final String PLC_DISK_CACHE_FILE = "plc_disk_cache.xml.gz";

	public final static double MAX_PLC_CACHE_AGE = 24;


	private Negative<Network> net = negative(Network.class);
	Positive<Timer> timer = positive(Timer.class);

	private double progress = 0.0;

	
	private boolean retryingWithCertIgnore = false;

	
	public PLCentralCacheComponent() {

		subscribe(handleGetRunningPlanetLabHosts, net);
	}

	private Handler<GetRunningPlanetLabHostsRequest> handleGetRunningPlanetLabHosts = new Handler<GetRunningPlanetLabHostsRequest>() {
		public void handle(GetRunningPlanetLabHostsRequest event) {

			// Use XML-RPC interface to Co-Mon to get the status of executing
			// hosts.
		}
	};


	private PlanetLabSite[] queryPLCSites(PlanetLabCredentials cred) {
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

		Object response = this.executeRPC(cred, "GetSites", params);
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

	private int[] queryPLCSliceNodes(PlanetLabCredentials cred) {

		Vector<Object> params = new Vector<Object>();
		params.add(cred.getPLCMap());

		HashMap<String, Object> filter = new HashMap<String, Object>();
		filter.put("name", cred.getSlice());
		params.add(filter);

		ArrayList<String> returnFields = new ArrayList<String>();
		returnFields.add("node_ids");
		params.add(returnFields);

		Object response = this.executeRPC(cred, "GetSlices", params);

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
	
	
	private PlanetLabStore sendPLCQueries(PlanetLabCredentials cred) {
		long startTime = System.currentTimeMillis();
		System.out.println("sending PLC queries");
		progress = 0.2;

		// query for hosts
		PlanetLabHost[] hosts = this.queryPLCHosts(cred);
		long time = System.currentTimeMillis() - startTime;
		System.out.println("PLC host query done: " + time + "ms");

		// start the dns resolver

		ParallelDNSLookup dnsLookups = new ParallelDNSLookup(
				DNS_RESOLVER_MAX_THREADS, hosts);
		dnsLookups.performLookups();

		progress = 0.4;
		PlanetLabSite[] sites = this.queryPLCSites(cred);
		time = System.currentTimeMillis() - startTime;
		System.out.println("PLC site query done: " + time + "ms");

		progress = 0.6;
		int[] sliceNodes = this.queryPLCSliceNodes(cred);
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
	
	private PlanetLabHost[] queryPLCHosts(PlanetLabCredentials cred) {
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
		Object response = this.executeRPC(cred, "GetNodes", params);
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

	
	private Object executeRPC(PlanetLabCredentials cred, String command, Vector<Object> params) {
		Object res = new Object();
		String url = PlanetLabConfiguration.getPlcApiAddress(); 

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
					return this.executeRPC(cred, command, params);
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

	private PlanetLabStore hostStoreReadFromDisk(PlanetLabCredentials cred) {
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
