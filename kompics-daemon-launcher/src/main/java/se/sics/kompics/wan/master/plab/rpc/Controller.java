package se.sics.kompics.wan.master.plab.rpc;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Start;
import se.sics.kompics.wan.master.plab.PLabComponent;
import se.sics.kompics.wan.master.plab.PLabHost;
import se.sics.kompics.wan.master.plab.PlanetLabCredentials;
import se.sics.kompics.wan.master.plab.plc.PLControllerInit;
import se.sics.kompics.wan.master.plab.plc.events.GetNodesForSliceRequest;
import se.sics.kompics.wan.master.ssh.Credentials;
import se.sics.kompics.wan.master.ssh.ExperimentHost;
import se.sics.kompics.wan.master.ssh.SshComponent;
import se.sics.kompics.wan.master.ssh.SshPort;
import se.sics.kompics.wan.master.ssh.events.SshConnectRequest;
import se.sics.kompics.wan.master.ssh.events.SshConnectResponse;

public class Controller extends ComponentDefinition {

	Negative<ControllerPort> ccPort = negative(ControllerPort.class);
	
	private static long startTime;
	
	private boolean errorInComponent = false;
	
	private Component pLabComponent;
	
	private Component sshComponent;

	private String homepage;
	
	private PlanetLabCredentials cred;
	
	private Vector<String> commandHistory = new Vector<String>();
	
	private List<PLabHost> connectedHosts = new CopyOnWriteArrayList<PLabHost>();	

	private List<PLabHost> availableHosts = new CopyOnWriteArrayList<PLabHost>();

	
	public Controller() {

		pLabComponent = create(PLabComponent.class);
		sshComponent = create(SshComponent.class);
		
		subscribe(handleGetRunningPlanetLabHosts, ccPort);
		subscribe(handleSshConnectRequest, ccPort);
		subscribe(handleSshConnectResponse, ccPort);
		
		subscribe(handleRpcInit, control);
		subscribe(handleStart, control);
	}

	private Handler<RpcInit> handleRpcInit = new Handler<RpcInit>() {
		public void handle(RpcInit event) {

			cred = event.getCredentials();
			PLControllerInit pInit = new PLControllerInit(cred);
			trigger(pInit, pLabComponent.getControl());
			homepage = event.getHomepage();
			
//			int maxThreads = event.getMaxThreads();
//	          WebServer webServer = new WebServer(port);
//	          
//	          XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();
//	        
//	          PropertyHandlerMapping phm = new PropertyHandlerMapping();
//	          Map<String,String> pMap = new HashMap<String,String>();
//	          pMap.put("PlanetLab", "se.sics.kompics.wan.master.plab.rpc.RpcFunctionsImpl");
//
//	          try {
//				phm.load(Thread.currentThread().getContextClassLoader(), pMap);
//			} catch (XmlRpcException e) {
//				e.printStackTrace();
//				errorInComponent = true;
//			}
//
//	          /* You may also provide the handler classes directly,
//	           * like this:
//	           * phm.addHandler("Calculator",
//	           *     org.apache.xmlrpc.demo.Calculator.class);
//	           * phm.addHandler(org.apache.xmlrpc.demo.proxy.Adder.class.getName(),
//	           *     org.apache.xmlrpc.demo.proxy.AdderImpl.class);
//	           */
//	          xmlRpcServer.setHandlerMapping(phm);
//	        
//	          XmlRpcServerConfigImpl serverConfig =
//	              (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
//	          serverConfig.setEnabledForExtensions(true);
//	          serverConfig.setContentLengthOptional(false);
//	          xmlRpcServer.setConfig(serverConfig);
//
//	          System.out.println("XML-RPC server is listening on port: " + port);
//
//	          webServer.acceptClient(ip.toString());
//	          
//	          try {
//				webServer.start();
//			} catch (IOException e) {
//				e.printStackTrace();
//				errorInComponent = true;
//			}

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
	

	private Handler<GetNodesForSliceRequest> handleGetRunningPlanetLabHosts = new Handler<GetNodesForSliceRequest>() {
		public void handle(GetNodesForSliceRequest event) {

		}
	};
	
	private Handler<SshConnectRequest> handleSshConnectRequest = 
		new Handler<SshConnectRequest>() {
		public void handle(SshConnectRequest event) {

			trigger(event, sshComponent.getNegative(SshPort.class));
		}
	};
	
	public Handler<SshConnectResponse> handleSshConnectResponse = 
		new Handler<SshConnectResponse>() {
		public void handle(SshConnectResponse event) {
			int sessionId = event.getSessionId();
			
			ExperimentHost host = event.getHostname();
			host.setSessionId(sessionId);
			PLabHost plHost = new PLabHost(host);
			
			connectedHosts.add(plHost);
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
