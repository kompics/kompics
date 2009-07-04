package se.sics.kompics.wan.master.plab.rpc;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.wan.master.plab.plc.events.GetNodesForSliceRequest;
import se.sics.kompics.wan.master.ssh.Credentials;

public class RpcComponent extends ComponentDefinition {

	private Negative<Network> net = negative(Network.class);
	Positive<Timer> timer = positive(Timer.class);

	public RpcComponent() {

		subscribe(handleGetRunningPlanetLabHosts, net);
	}

	private Handler<GetNodesForSliceRequest> handleGetRunningPlanetLabHosts = new Handler<GetNodesForSliceRequest>() {
		public void handle(GetNodesForSliceRequest event) {

			// Use XML-RPC interface to Co-Mon to get the status of executing
			// hosts.
		}
	};

	private Object executeObjectCommand(XmlRpcClient client, String command, Object[] params) 
	{
		Object result = null;

		try {
			result = (Object) client.execute(command, params);
			// System.out.print("" + result);
		} catch (XmlRpcException e) {
			System.err.println("failure while executing '" + command + "': " + e.getMessage());
			return null;
		}
		// System.out.println(command + ": " + result);
		return result;
	}

	public XmlRpcClient startClient(InetAddress server, Credentials cred, int port)
			throws MalformedURLException {
		// this.authCred = cred;
		XmlRpcClient client = null;
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		String path = "http://" + server.getHostAddress() + ":" + port + "/xmlrpc";
		;
		config.setServerURL(new URL(path));
		System.out.println(path);

		client = new XmlRpcClient();
		client.setConfig(config);
		return client;
	}
}
