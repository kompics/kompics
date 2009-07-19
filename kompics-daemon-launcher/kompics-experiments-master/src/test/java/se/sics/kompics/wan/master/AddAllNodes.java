package se.sics.kompics.wan.master;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

public class AddAllNodes extends Thread {

	private int numNodes = -1;

	private boolean done;

	private String statusText = "";

	String slice = "sics_gradient";

	String myPlcUsername = "jdowling";

	String myPlcPassword = "...";

	public String getStatusText() {
		return statusText;
	}

	public boolean isDone() {
		return done;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args.length != 2) {
			System.out
					.println("usage: java -jar AddAllNodes.jar <slice> <username(e-mail)>");
			System.exit(1);
		}
		AddAllNodes t = new AddAllNodes(args[0], args[1]);
		t.run();
	}

	public AddAllNodes(String slice, String myPlcUsername) {

		String myPlcPassword = this.promtForPassword();
		new AddAllNodes(slice, myPlcUsername, myPlcPassword);

	}

	public AddAllNodes(String slice, String myPlcUsername, String myPlcPassword) {
		this.slice = slice;
		this.myPlcUsername = myPlcUsername;
		this.myPlcPassword = myPlcPassword;
	}

	@SuppressWarnings("unchecked")
	public void run() {
		try {

			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();

			config.setServerURL(new URL("https://www.planet-lab.org/PLCAPI/"));
			config.setEncoding("iso-8859-1");
			config.setEnabledForExtensions(true);

			XmlRpcClient client = new XmlRpcClient();
			//client.setTypeFactory(new MyTypeFactory(client));

			client.setConfig(config);
			Hashtable auth = new Hashtable();
			auth.put("Username", myPlcUsername);
			auth.put("AuthMethod", "password");
			auth.put("AuthString", myPlcPassword);
			auth.put("Role", "user");

			Vector nodes = new Vector();
			Vector fields = new Vector();
			fields.add("hostname");

			Vector params = new Vector();
			params.add(auth);
			params.add(nodes);
			params.add(fields);

			/* get all planetlab nodes */

			statusText = "sending query for all nodes";
			System.out.println(statusText);
			Object[] res = (Object[]) client.execute("AdmGetNodes", params);

			Vector allNodes = new Vector();
			for (int i = 0; i < res.length; i++) {
				HashMap hash = (HashMap) res[i];
				String hostname = (String) hash.get("hostname");
				allNodes.add(hostname);
				// System.out.println(hostname);
			}
			this.numNodes = allNodes.size();
			statusText = "got " + allNodes.size() + " nodes, adding";
			System.out.println(statusText);
			params = new Vector();
			params.add(auth);
			params.add(slice);
			params.add(allNodes);

			//client.execute("SliceNodesAdd",params);

			statusText = "done, added " + numNodes;
			System.out.println(statusText);
			this.done = true;

		} catch (XmlRpcException e) {
			// TODO Auto-generated catch block
			statusText = "Logon failure";
			//e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		done=true;
	}

	private String promtForPassword() {
		BufferedReader stdin = new BufferedReader(new InputStreamReader(
				System.in));
		String password = null;
		try {
			ConsoleEraser consoleEraser = new ConsoleEraser();
			System.out.println("Enter MyPLC password:");
			consoleEraser.start();
			password = stdin.readLine();
			consoleEraser.halt();
			System.out.print("\b");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return password;
	}

	private class ConsoleEraser extends Thread {
		private boolean running = true;

		public void run() {
			while (running) {
				System.out.print("\b ");
			}
		}

		public synchronized void halt() {
			running = false;
		}
	}

//	private class MyTypeFactory extends TypeFactoryImpl {
//		public MyTypeFactory(XmlRpcController pController) {
//			super(pController);
//		}
//
//		public TypeParser getParser(XmlRpcStreamConfig pConfig,
//				NamespaceContextImpl pContext, String pURI, String pLocalName) {
//
//			// System.out.println("pURI: pURI" + " plocalName: " + pLocalName);
//			return super.getParser(pConfig, pContext, pURI, pLocalName);
//
//		}
//
//		public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig,
//				Object pObject) throws SAXException {
//			return super.getSerializer(pConfig, pObject);
//		}
//	}
}
