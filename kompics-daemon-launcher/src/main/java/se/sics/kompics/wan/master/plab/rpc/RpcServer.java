package se.sics.kompics.wan.master.plab.rpc;


import java.io.IOException;
import java.util.concurrent.Semaphore;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfig;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.master.plab.Credentials;
import se.sics.kompics.wan.master.plab.PlanetLabCredentials;
import se.sics.kompics.wan.master.ssh.ConnectionController;

import se.sics.kompics.wan.master.ssh.ConnectionController;

//import edu.washington.cs.pl_if.Constants;
//import edu.washington.cs.pl_if.Credentials;
//import edu.washington.cs.pl_if.Main;
//import edu.washington.cs.pl_if.gui.GuiMain;
//import edu.washington.cs.pl_if.ssh.ConnectionController;

public class RpcServer {

	private static RpcServer instance = null;

	private static long startTime = System.currentTimeMillis();

	private WebServer webServer;

	private Credentials authCred = null;

	private ConnectionController controller;

	// private ConcurrentHashMap<Credentials, ConnectionController> controllers;

	// creates the queue usd to limit the number of concurrent network intensive
	// threads. The queue is fair meaning that it operarates in a FIFO manner
	private Semaphore networkIntensiveTicket = new Semaphore(0, true);

	public static RpcServer getInstance() {
		if (instance == null) {
			instance = new RpcServer();
		}

		return instance;
	}

	/**
	 * Get relative time since the application started. Since the XML-RPC
	 * standard has no support for 64 bit integers (unless extensions are used),
	 * a double is used.
	 * 
	 * @return number of seconds since application start
	 */

	public static double getTime() {
		long currentTime = System.currentTimeMillis();

		double time = (currentTime - startTime) / 1000.0;
		return time;
	}

	public void startController(PlanetLabCredentials cred){
		if(controller == null){
			System.out.println("Starting controller " + cred.toString());
			this.authCred = cred;
			controller = new ConnectionController(cred);
		}
	}
	
	protected RpcServer() {
		System.out.println("creating rpc server");
		startTime = System.currentTimeMillis();
		try {

			// controllers = new ConcurrentHashMap<Credentials,
			// ConnectionController>();

			int port;
			port = PlanetLabConfiguration.getLocalXmlRpcPort(); 
				//String portString = Main.getConfig(Constants.LOCAL_XML_RPC_PORT);
//			if(portString != null){
//				port = Integer.parseInt(portString);
//			} else {
//				port = Constants.LOCAL_XML_RPC_PORT_DEFAULT;
//			}
			
			final String propertyFile = "RpcServer.properties";

			//System.out.println("loading properties");
			PropertyHandlerMapping mapping = new PropertyHandlerMapping();
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			mapping.load(cl, propertyFile);
			//System.out.println("loaded properties");
			webServer = new WebServer(port);

			// Simple access control for now, allow only localhost
			// TODO: make the webserver use ssl
			// webServer.setParanoid(true);
			webServer.acceptClient("127.0.0.1");

			XmlRpcServerConfig config = new XmlRpcServerConfigImpl();
			XmlRpcServer server = webServer.getXmlRpcServer();

			server.setConfig(config);
			server.setHandlerMapping(mapping);

			System.out.println("starting rpc-server, listening on port: " + port);
			try {
				webServer.start();
			} catch (java.net.BindException e) {
				System.err.println("Unable to start XML-RPC server on port "
						+ port + ": " + e.getMessage());
				this.shutdown();
				System.exit(1);
			}
		} catch (IOException e) {
			System.out.print(e.getMessage());
			e.printStackTrace();
		} catch (XmlRpcException e) {
			System.out.print(e.getMessage());
			e.printStackTrace();
		}
		System.out.print("started rpc server");
	}

	public boolean checkCredentials(Credentials cred) {
		return authCred.authenticate(cred);
	}

	public ConnectionController getController(Credentials cred) {

		// Verify if this request passes the authentication check
		if (!checkCredentials(cred)) {
			// oups, intruder!!! :-)
			System.err.println("auth problem, " + cred.getUsername());
			return null;
		}

		if (controller == null) {
			System.err.println("controller not started");
		}

		return controller;

		// check if we already have a connecion controller open for this
		// if (controllers.containsKey(cred)) {
		// ConnectionController controller = controllers.get(cred);
		//
		// if (controller.getCredentials().equals(cred)) {
		// return controller;
		// }
		// } else {
		// // start a new controller with the provided credentials
		//
		// ConnectionController controller = new ConnectionController(cred);
		// controllers.put(cred, controller);
		// System.out.println("Starting controller " + controllers.size()
		// + "cred=" + cred.toString());
		//
		// return controller;
		// }
		// return null;
	}

	/**
	 * used to controll how many threads that can run network intensive stuff
	 * controlling for example the number of concurrent scp connections when
	 * uploading and downloading content
	 * 
	 * @throws InterruptedException
	 */

	public int getNetworkIntensiveTicket() throws InterruptedException {
		networkIntensiveTicket.acquire();
		return networkIntensiveTicket.availablePermits();
	}

	public void halt() {
		webServer.shutdown();
	}

	/**
	 * increase the number of concurrent network intensive threads that are
	 * allowed to run. Note that this only can be increased
	 * 
	 * @param newLimit
	 *            the new limit
	 */
	public void increaseConcurrentCopyLimit(int newLimit) {
		int currentLimit = networkIntensiveTicket.getQueueLength()
				+ networkIntensiveTicket.availablePermits();
		int diff = newLimit - currentLimit;
		if (currentLimit < newLimit) {
			networkIntensiveTicket.release(diff);
		} else {
			// only increase is allowed
		}
		return;
	}

	public int releaseNetworkIntensiveTicket() {
		networkIntensiveTicket.release();
		return networkIntensiveTicket.availablePermits();
	}

	public int shutdown(Credentials cred) {
		if (this.checkCredentials(cred)) {
			this.shutdown();
			return 0;
		}
		return -1;
	}

	private void shutdown() {

//		if (GuiMain.running) {
//			GuiMain.getInstance().kill();
//		}
		// shut down the webServer
		webServer.shutdown();

		controller.shutdown();
		// close all connections
		// for (Iterator iter = controllers.values().iterator();
		// iter.hasNext();) {
		// ConnectionController controller = (ConnectionController) iter
		// .next();
		// controller.shutdown();
		//
		// }

	}
}
