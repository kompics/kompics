package se.sics.kompics.wan.master.plab.rpc;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.master.plab.plc.events.GetNodesForSliceRequest;
import se.sics.kompics.wan.master.ssh.Credentials;
import se.sics.kompics.wan.web.Web;
import se.sics.kompics.wan.web.WebRequest;
import se.sics.kompics.wan.web.WebResponse;
import se.sics.kompics.wan.web.jetty.JettyWebServer;
import se.sics.kompics.wan.web.jetty.JettyWebServerInit;

public class RpcComponent extends ComponentDefinition {

	private Negative<Network> net = negative(Network.class);
	Positive<Timer> timer = positive(Timer.class);

	// private Negative<Web> web = negative(Web.class);
	private Component webServer;

	private static long startTime;
	
	public RpcComponent() {

		webServer = create(JettyWebServer.class);

		subscribe(handleGetRunningPlanetLabHosts, net);
		subscribe(handleWebRequest, webServer.getNegative(Web.class));
		
		subscribe(handleRpcInit, control);
		subscribe(handleStart, control);
	}

	private Handler<RpcInit> handleRpcInit = new Handler<RpcInit>() {
		public void handle(RpcInit event) {

			InetAddress ip = event.getIp();
			PlanetLabConfiguration.getXmlRpcPort();
			int port =  event.getPort();
			int requestTimeout = event.getRequestTimeout();
			int maxThreads = event.getMaxThreads();
			String homepage= event.getHomepage();
			JettyWebServerInit initWeb = new JettyWebServerInit(ip, port,
					requestTimeout, maxThreads, homepage);
			trigger(initWeb, webServer.getControl());
		}
	};


	private Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {
			startTime = System.currentTimeMillis();
			
			
		}
	};
	
	/**
	 * XML-RPC has no support for 64 bit integers (unless extensions are used),
	 * so a double is used.
	 */
	public static double getTime() {
		long currentTime = System.currentTimeMillis();

		double time = (currentTime - startTime) / 1000.0;
		return time;
	}
	
	private Handler<WebRequest> handleWebRequest = new Handler<WebRequest>() {
		public void handle(WebRequest event) {

			// XXX
			
			trigger(new WebResponse("some html", event, 0, 1), webServer
					.getPositive(Web.class));
		}
	};

	private Handler<GetNodesForSliceRequest> handleGetRunningPlanetLabHosts = new Handler<GetNodesForSliceRequest>() {
		public void handle(GetNodesForSliceRequest event) {

			// Use XML-RPC interface to Co-Mon to get the status of executing
			// hosts.
		}
	};

	private Object executeObjectCommand(XmlRpcClient client, String command,
			Object[] params) {
		Object result = null;

		try {
			result = (Object) client.execute(command, params);
			// System.out.print("" + result);
		} catch (XmlRpcException e) {
			System.err.println("failure while executing '" + command + "': "
					+ e.getMessage());
			return null;
		}
		// System.out.println(command + ": " + result);
		return result;
	}

	public XmlRpcClient startClient(InetAddress server, Credentials cred,
			int port) throws MalformedURLException {
		// this.authCred = cred;
		XmlRpcClient client = null;
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		String path = "http://" + server.getHostAddress() + ":" + port
				+ "/xmlrpc";
		;
		config.setServerURL(new URL(path));
		System.out.println(path);

		client = new XmlRpcClient();
		client.setConfig(config);
		return client;
	}
}
