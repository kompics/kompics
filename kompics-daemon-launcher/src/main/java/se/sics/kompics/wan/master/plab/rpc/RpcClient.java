package se.sics.kompics.wan.master.plab.rpc;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.master.ssh.Credentials;

public class RpcClient implements RpcFunctions {

	public static final String PLANET_LAB_GET_SUCCESSFUL_HOSTS = "PlanetLab.getSuccessfulHosts";

	private static RpcClient instance = new RpcClient();

	private final String XMLRPC_PATH;

	private XmlRpcClient client;

	private Credentials authCred = null;

	public static RpcClient getInstance() {
		return instance;

	}

	protected RpcClient() {
//		String portString = Main.getConfig(Constants.LOCAL_XML_RPC_PORT);
//		String portString = Constants.LOCAL_XML_RPC_PORT;
		int port = PlanetLabConfiguration.getXmlRpcPort();
//		if (portString != null) {
//			port = Integer.parseInt(portString);
//		} else {
//			port = Constants.LOCAL_XML_RPC_PORT_DEFAULT;
//		}
		XMLRPC_PATH = ":" + port + "/xmlrpc";
	}

	public int addRandomHostsFromPLC(int numberOfHosts) {
		return this.addRandomHostsFromPLC(authCred.getAuthMap(), numberOfHosts);
	}

	public int addRandomHostsFromPLC(Map auth, int numberOfHosts) {
		Object[] params = new Object[] { auth, numberOfHosts };
		return this.executeIntCommand(
				RpcFunctions.PLANET_LAB_ADD_RANDOM_HOSTS_FROM_PLC, params);
	}

	public int addRandomSitesFromPLC(int numberOfSites) {
		return this.addRandomSitesFromPLC(authCred.getAuthMap(), numberOfSites);
	}

	public int addRandomSitesFromPLC(Map auth, int numberOfSites) {
		Object[] params = new Object[] { auth, numberOfSites };
		return this.executeIntCommand(
				RpcFunctions.PLANET_LAB_ADD_RANDOM_SITES_FROM_PLC, params);
	}

	public double commandCompleted(Map auth, int commandId) {
		System.err.println("unimplemented: commandCompleted");

		return 0;
	}

	public int connectToHost(Map auth, String hostname) {
		Object[] params = new Object[] { auth, hostname };
		return this.executeIntCommand(RpcFunctions.PLANET_LAB_CONNECT_TO_HOST,
				params);
	}

	public int connectToHost(String hostname) {
		return this.connectToHost(authCred.getAuthMap(), hostname);
	}

	public String getCommand(int commandIndex) {
		return this.getCommand(authCred.getAuthMap(), commandIndex);
	}

	public String getCommand(Map auth, int commandIndex) {
		Object[] params = new Object[] { auth, commandIndex };
		return this.executeStringCommand(RpcFunctions.PLANET_LAB_GET_COMMAND,
				params);
	}

	public Object[] getCommandOverview(int commandId) {
		return this.getCommandOverview(authCred.getAuthMap(), commandId);
	}

	public Object[] getCommandOverview(Map auth, int commandId) {
		Object[] params = new Object[] { auth, commandId };
		return (Object[]) this.executeObjectArrayCommand(
				RpcFunctions.PLANET_LAB_GET_COMMAND_OVERVIEW, params);
	}

	public Object[] getCommandStats(int connectionId) {
		return this.getCommandStats(authCred.getAuthMap(), connectionId);
	}

	public Object[] getCommandStats(Map auth, int connectionId) {
		Object[] params = new Object[] { auth, connectionId };
		return (Object[]) this.executeObjectArrayCommand(
				RpcFunctions.PLANET_LAB_GET_COMMAND_STATS, params);
	}

	public Object[] getCommandStatusOverview() {
		return this.getCommandStatusOverview(authCred.getAuthMap());
	}

	public Object[] getCommandStatusOverview(Map auth) {
		Object[] params = new Object[] { auth };
		return (Object[]) this.executeObjectArrayCommand(
				RpcFunctions.PLANET_LAB_GET_COMMAND_STATUS_OVERVIEW, params);
	}

	public Map getExitStats(Map auth, int commandId) {
		System.err.println("unimplemented: getExitStats");
		return null;
	}

	public String getHostname(int hostnum) {
		return this.getHostname(authCred.getAuthMap(), hostnum);
	}

	public String getHostname(Map auth, int hostnum) {
		Object[] params = new Object[] { auth, hostnum };
		return this.executeStringCommand(RpcFunctions.PLANET_LAB_GET_HOSTNAME,
				params);
	}

	public Object[] getHostStatusOverview() {
		return this.getHostStatusOverview(authCred.getAuthMap());
	}

	public Object[] getHostStatusOverview(Map auth) {
		Object[] params = new Object[] { auth };
		return (Object[]) this.executeObjectArrayCommand(
				RpcFunctions.PLANET_LAB_GET_HOST_STATUS_OVERVIEW, params);
	}

	public int queueCommand(Map auth, String command) {
		return this.queueCommand(auth, command, 0.0, false);
	}

	public int queueCommand(Map auth, String command, double timeout) {
		return this.queueCommand(auth, command, timeout, false);
	}

	public int queueCommand(Map auth, String command, double timeout,
			boolean stopOnError) {
		Object[] params = new Object[] { auth, command, timeout, stopOnError };
		return this.executeIntCommand(RpcFunctions.PLANET_LAB_QUEUE_COMMAND,
				params);
	}

	public int queueCommand(Map auth, String command, double timeout,
			int stopOnError) {
		boolean stop = false;
		if (stopOnError > 0) {
			stop = true;
		}
		return this.queueCommand(auth, command, timeout, stop);
	}

	public int queueCommand(Map auth, String command, int timeout) {
		return this.queueCommand(auth, command, timeout, false);
	}

	public int queueCommand(String command) {
		return this.queueCommand(authCred.getAuthMap(), command);
	}

	public Object[] readConsole(int connectionID, int fromRow) {
		return this.readConsole(authCred.getAuthMap(), connectionID, fromRow);
	}

	public Object[] readConsole(Map auth, int connectionID, int fromRow) {
		Object[] params = new Object[] { auth, connectionID, fromRow };
		return (Object[]) this.executeObjectArrayCommand(
				RpcFunctions.PLANET_LAB_READ_CONSOLE, params);
	}

	public void setCredentials(Credentials cred) {
		this.authCred = cred;
		// this.connectToHost("planetlab01");

	}

	public int shutdown() {
		return this.shutdown(authCred.getAuthMap());
	}

	public int shutdown(Map auth) {
		Object[] params = new Object[] { auth };
		return this.executeIntCommand(RpcFunctions.PLANET_LAB_SHUTDOWN, params);
	}

	public void startClient(InetAddress server, Credentials cred) {
		this.authCred = cred;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			String path = "http://" + server.getHostAddress() + XMLRPC_PATH;
			config.setServerURL(new URL(path));
			System.out.println(path);

			client = new XmlRpcClient();
			client.setConfig(config);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public int totalCommandNum() {
		return this.totalCommandNum(authCred.getAuthMap());
	}

	public int totalCommandNum(Map auth) {
		Object[] params = new Object[] { auth };
		return this.executeIntCommand(
				RpcFunctions.PLANET_LAB_TOTAL_COMMAND_NUM, params);
	}

	public int totalHostNum() {
		return this.totalHostNum(authCred.getAuthMap());
	}

	public int totalHostNum(Map auth) {
		Object[] params = new Object[] { auth };
		return this.executeIntCommand(RpcFunctions.PLANET_LAB_TOTAL_HOST_NUM,
				params);
	}

	public int upload(Map auth, String path, double timeout, boolean stopOnError) {
		Object[] params = new Object[] { auth, path, timeout, stopOnError };
		return this.executeIntCommand(RpcFunctions.PLANET_LAB_UPLOAD, params);
	}

	public int upload(Map auth, String path, double timeout, int stopOnError) {
		boolean stop = false;
		if (stopOnError > 0) {
			stop = true;
		}
		return this.upload(auth, path, timeout, stop);
	}

	public int upload(String path) {
		return this.upload(authCred.getAuthMap(), path, 0, false);
	}

	private Object executeObjectCommand(String command, Object[] params) {
		synchronized (client) {

			Object result = null;

			try {
				result = (Object) client.execute(command, params);
				// System.out.print("" + result);
			} catch (XmlRpcException e) {
				System.err.println("failure while executing '" + command
						+ "': " + e.getMessage());
				return null;
			}
			// System.out.println(command + ": " + result);
			return result;
		}
	}

	/**
	 * Executes a XML-RPC command that returns an integer
	 * 
	 * @param command
	 *            the XML-RPC command
	 * @param params
	 *            tha parameters
	 * @return the return code, or Integer.MIN_VALUE on failure
	 */
	private int executeIntCommand(String command, Object[] params) {
		Integer result = (Integer) this.executeObjectCommand(command, params);

		if (result == null) {
			return Integer.MIN_VALUE;
		}
		return result;

	}

	private double executeDoubleCommand(String command, Object[] params) {
		Double result = (Double) this.executeObjectCommand(command, params);

		if (result == null) {
			return Double.MIN_VALUE;
		}
		return result;

	}

	/**
	 * Executes a XML-RPC command that returns an Object[]
	 * 
	 * @param command
	 *            the XML-RPC command
	 * @param params
	 *            tha parameters
	 * @return the return Object[], or null on failure
	 */

	private Object[] executeObjectArrayCommand(String command, Object[] params) {
		synchronized (client) {

			Object[] result;

			try {
				result = (Object[]) client.execute(command, params);
				// System.out.print("" + result);
			} catch (XmlRpcException e) {

				System.err.println("failure while executing '" + command
						+ "': " + e.getMessage());
				return new Object[0];
			}
			return result;
		}
	}

	private Map executeMapCommand(String command, Object[] params) {

		Map result = (Map) this.executeObjectCommand(command, params);
		return result;

	}

	private String executeStringCommand(String command, Object[] params) {
		String result = (String) this.executeObjectCommand(command, params);
		return result;
	}

	public Object[] getConnectedHosts() {
		return this.getConnectedHosts(authCred.getAuthMap());
	}

	public Object[] getConnectedHosts(Map auth) {
		return this.getSuccessfulHosts(auth, 0);
	}

	public Object[] getSuccessfulHosts(int commandId) {
		return this.getSuccessfulHosts(authCred.getAuthMap(), commandId);
	}

	public Object[] getSuccessfulHosts(Map auth, int commandId) {
		Object[] params = new Object[] { auth, commandId };
		return this.executeObjectArrayCommand(PLANET_LAB_GET_SUCCESSFUL_HOSTS,
				params);
	}

	public int kill(Map auth, int commandId) {
		Object[] params = new Object[] { auth, commandId };
		return this.executeIntCommand(RpcFunctions.PLANET_LAB_KILL_COMMAND,
				params);
	}

	public int kill(int commandId) {
		return this.kill(authCred.getAuthMap(), commandId);
	}

	public int download(Map auth, String remotePath, String localPath,
			String fileFilter, String localNamingType) {
		Object[] params = new Object[] { auth, remotePath, localPath,
				fileFilter, localNamingType };

		return this.executeIntCommand(RpcFunctions.PLANET_LAB_DOWNLOAD, params);
	}

	public int download(String remotePath, String localPath, String fileFilter,
			String localNamingType) {
		return this.download(authCred.getAuthMap(), remotePath, localPath,
				fileFilter, localNamingType);

	}

	public int addHostsToSlice(Map auth, Object[] hosts) {
		Object[] params = new Object[] { auth, hosts };
		return this.executeIntCommand(
				RpcFunctions.PLANET_LAB_ADD_HOSTS_TO_SLICE, params);
	}

	public int addHostsToSlice(Object[] hosts) {
		return this.addHostsToSlice(authCred.getAuthMap(), hosts);
	}

	public Object[] getHostsNotInSlice(Map auth) {
		Object[] params = new Object[] { auth };
		return this.executeObjectArrayCommand(
				RpcFunctions.PLANET_LAB_GET_HOSTS_NOT_IN_SLICE, params);
	}

	public Object[] getHostsNotInSlice() {
		return this.getHostsNotInSlice(authCred.getAuthMap());
	}

	public int fetchCoMonData(Map auth) {
		Object[] params = new Object[] { auth };
		return this.executeIntCommand(RpcFunctions.PLANET_LAB_FETCH_COMON_DATA,
				params);
	}

	public int fetchCoMonData() {
		return this.fetchCoMonData(authCred.getAuthMap());
	}

	public double getCoMonProgress(Map auth) {
		Object[] params = new Object[] { auth };
		return this.executeDoubleCommand(
				RpcFunctions.PLANET_LAB_GET_COMON_PROGRESS, params);
	}

	public double getCoMonProgress() {
		return this.getCoMonProgress(authCred.getAuthMap());
	}

	public Object[] getHostStats(Map auth, Object[] hosts) {
		Object[] params = new Object[] { auth, hosts };
		return this.executeObjectArrayCommand(
				RpcFunctions.PLANET_LAB_GET_HOST_STATS, params);
	}

	public Object[] getHostStats(Object[] hosts) {
		return this.getHostStats(authCred.getAuthMap(), hosts);
	}

	public Object[] getAllHosts(Map auth) {
		Object[] params = new Object[] { auth };
		return this.executeObjectArrayCommand(
				RpcFunctions.PLANET_LAB_GET_ALL_HOSTS, params);
	}

	public Object[] getAllHosts() {
		return this.getAllHosts(authCred.getAuthMap());
	}

	public Object[] getAvailableHosts(Map auth) {
		Object[] params = new Object[] { auth };
		return this.executeObjectArrayCommand(
				RpcFunctions.PLANET_LAB_GET_AVAILABLE_HOSTS, params);
	}

	public Object[] getAvailableHosts() {
		return this.getAvailableHosts(authCred.getAuthMap());
	}
}
