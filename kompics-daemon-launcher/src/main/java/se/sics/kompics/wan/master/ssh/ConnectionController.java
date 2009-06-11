package se.sics.kompics.wan.master.ssh;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import se.sics.kompics.wan.master.plab.Credentials;
import se.sics.kompics.wan.master.plab.plc.PLCentralController;
import se.sics.kompics.wan.master.plab.plc.PlanetLabHost;
import se.sics.kompics.wan.master.plab.plc.PlanetLabSite;
import se.sics.kompics.wan.master.plab.rpc.RpcFunctions;


public class ConnectionController extends Thread {

	private Credentials credentials;

	private Vector<SshConnection> connections = new Vector<SshConnection>();

	private Vector<String> commandHistory = new Vector<String>();

	private ConnectThread connectThread;

	private PLCentralController plcConnection;

	public ConnectionController(Credentials cred) {

		// This is the credentials that will be used, all functions will run as
		// this
		this.credentials = cred;
		// initiate the connection with planetlabcentral
		plcConnection = new PLCentralController(cred);
		// ugly hack to make the gui "consistant"
		commandHistory.add("#connect");
		// create the connect thread
		connectThread = new ConnectThread();
		connectThread.start();
	}

	/**
	 * requests num randomly selected hosts from PLC and tries to connect to
	 * them. The policy used if the connect attempt fails is "any", meaning that
	 * it will try another host instead
	 * 
	 * @param num
	 *            number of hosts to add
	 * @return returns the total number of hosts
	 */

	public int addRandomHostsFromPLC(int num) {
		PlanetLabHost[] hosts = plcConnection.getRandomNodes(num);
		for (int i = 0; i < hosts.length; i++) {

			connectToHost(hosts[i], RpcFunctions.CONNECT_FAILED_POLICY_ANY);
		}

		return this.getHostNum();
	}

	/**
	 * requests num randomly selected sites from PLC and tries to connect to a
	 * random host in each of the sites them. The hosts are connected with the
	 * "site" policy on unsuccessful connections, meaning that if there is an
	 * error while conecting to the host, another host from the same site will
	 * be tried
	 * 
	 * @param num
	 *            number of hosts to add
	 * @return returns the total number of hosts
	 */

	public int addRandomSitesFromPLC(int num) {
		PlanetLabHost[] hosts = plcConnection.getRandomSites(num);
		for (int i = 0; i < hosts.length; i++) {

			connectToHost(hosts[i], RpcFunctions.CONNECT_FAILED_POLICY_SITE);
		}

		return this.getHostNum();
	}

	/**
	 * initiate the connection sequece to host "host"
	 * 
	 * @param host
	 *            the fully qualified hostname
	 * @param connectFailedPolicy
	 *            the policy to use if the connect fails
	 * @return the connection id of the host
	 */
	public int connectToHost(PlanetLabHost host, String connectFailedPolicy) {
		int hostId = -1;
		// make sure we are not currently connected to the host
		if (host.getConnectionId() < 0) {

			SshConnection conn = new SshConnection(this, host);
			connections.add(conn);

			hostId = connections.size() - 1;
			host.setConnectionId(hostId);
			host.setConnectFailedPolicy(connectFailedPolicy);
			connectThread.connect(conn);
			synchronized (commandHistory) {
				for (int i = 1; i < commandHistory.size(); i++) {
					conn.queueCommand(commandHistory.get(i));
				}
			}

		} else {
			// I guess we have to find the connetionId...
			hostId = host.getConnectionId();
		}
		return hostId;
	}

	/**
	 * Disconnect from all hosts
	 * 
	 */
	public void disconnectAll() {
		for (int i = 0; i < connections.size(); i++) {
			connections.get(i).halt();
		}
	}

	/**
	 * disconnect one host specified with the connection ID
	 * 
	 * @param connectionId
	 *            the id of the connection
	 * @return true if the connection id is valid, false otherwise
	 */
	public boolean disconnectHost(int connectionId) {
		if (connectionId > 0 && connectionId < connections.size()) {
			SshConnection conn = connections.get(connectionId);
			conn.halt();
			return true;
		}
		return false;
	}

	/**
	 * returns the command string assiciated with the specified command id
	 * 
	 * @param commandId
	 *            the id of the command
	 * @return the command, or null if no command exists with that command id
	 */
	public String getCommand(int commandId) {
		if (commandId >= 0 && commandId < commandHistory.size()) {
			return commandHistory.get(commandId);
		}
		return null;
	}

	/**
	 * Sends a query for exitcode stats for executed commands
	 * 
	 * @param commandIndex
	 *            the index of the command queried
	 * @return a hashtable<Integer exitcode,Integer count>
	 */
	public Map<Integer, Integer> getCommandExitStats(int commandIndex) {
		Map<Integer, Integer> exitcodes = new HashMap<Integer, Integer>();
		// int[] codeCounts = new int[256];
		for (Iterator iter = connections.iterator(); iter.hasNext();) {
			SshConnection conn = (SshConnection) iter.next();
			if (conn.isCommandCompleted(commandIndex)) {
				int exitCode = conn.getCommandExitStatus(commandIndex);
				// codeCounts[exitCode]++;
				if (!exitcodes.containsKey(exitCode)) {
					exitcodes.put(exitCode, 0);
				}
				exitcodes.put(exitCode, exitcodes.get(exitCode) + 1);

			}
		}

		return exitcodes;
	}

	/**
	 * get the total number of commands queued and executed
	 * 
	 * @return the total number of commands executed and queued
	 */
	public int getCommandNum() {
		return commandHistory.size();
	}

	/**
	 * queries for what ratio of hosts that have completed the command specified
	 * by command id
	 * 
	 * @param commandId
	 *            the command id
	 * @return double between 0 and 1, 1 means that all hosts have completed the
	 *         command
	 */
	public double getCompletionStats(int commandId) {
		synchronized (connections) {

			int completed = 0;
			int total = connections.size();
			if (total == 0) {
				return 0.0;
			}

			for (Iterator iter = connections.iterator(); iter.hasNext();) {
				SshConnection conn = (SshConnection) iter.next();
				if (conn.wasConnected() || commandId == 0) {
					if (conn.isCommandCompleted(commandId)) {
						completed++;
					}
				}
			}

			// only count connected hosts to the total (if command 0 is
			// complete)
			if (commandId > 0) {

				for (SshConnection conn : connections) {
					if (conn.getCommandExitStatus(0) != 0 || conn.isHalted()) {
						total--;
					}
				}
			}
			// System.out.println(total);

			return ((double) completed) / ((double) total);
		}
	}

	/**
	 * get the credentials used
	 * 
	 * @return the credentials used
	 */
	public Credentials getCredentials() {
		return credentials;
	}

	
	/**
	 * returns a PlanetLabHost with the same ip as the ip of the host specified
	 * by hostname. Note that the hostname contained if the PlanetLabHost class
	 * might differ from the hostname specified as "hostname". The hostname in
	 * the PlanetLabHost class will always be the hostname provided by PLC, the
	 * fully qualified hostname.
	 * 
	 * @param hostname
	 *            The hostname of the host to query for, or the ip address in
	 *            quad dot format
	 * @return The PlanetLabHost with the same ip as the hostname resolves to,
	 *         or null if no planetlab host with that ip is found
	 */
	public PlanetLabHost getHostInfo(String hostname) {
		return plcConnection.getHost(hostname);
	}

	public PlanetLabHost getHostInfo(int plcHostId) {
		return plcConnection.getHost(plcHostId);
	}

	/**
	 * get the total number of hosts, including disconencted hosts
	 * 
	 * @return total number of hosts
	 */
	public int getHostNum() {
		return connections.size();
	}

	public PlanetLabHost getPlanetLabHost(int connectionId) {
		if (connectionId < 0 || connectionId >= this.getHostNum()) {
			return null;
		}

		return connections.get(connectionId).getHost();
	}

	public void getCoMonData() {
		PlanetLabHost[] allHosts = plcConnection.getAllHosts();
		plcConnection.setCoMonData(allHosts);
	}

	public double getCoMonProgress() {
		return plcConnection.getCoMonProgress();
	}

	public PlanetLabSite getPlanetLabSite(PlanetLabHost host) {
		return plcConnection.getSite(host);
	}

	public PlanetLabHost[] getHostsNotInSlice() {
		return plcConnection.getHostsNotInSlice();
	}

	public PlanetLabHost[] getAllHosts() {
		return plcConnection.getAllHosts();
	}

	public PlanetLabHost[] getAvailableHosts() {
		return plcConnection.getAvailableHosts();
	}

	public int addHostsToSlice(String[] hostnames) {
		return plcConnection.addHostsToSlice(hostnames);
	}

	// public String getLastLine(int connectionId) {
	// return connections.get(connectionId).getLastOutput();
	// }

	public SshConnection getSshConnection(int connectionId) {
		if (connectionId < 0 || connectionId >= connections.size()) {
			return null;
		}
		return connections.get(connectionId);
	}

	public void handleFailedConnect(PlanetLabHost host) {
		String fullPolicy = host.getConnectFailedPolicy();
		String[] policySplit = fullPolicy.split(",");
		// System.out.println(fullPolicy + ";" + policySplit.length);
		for (int i = 0; i < policySplit.length; i++) {

			String discPolicy = policySplit[i];
			if (discPolicy.equals(RpcFunctions.CONNECT_FAILED_POLICY_ANY)) {
				// connect to any host
				PlanetLabHost[] newHost = plcConnection.getRandomNodes(1);

				// if we found a new host
				if (newHost.length > 0) {
					// connect to it, with the same policy as the current host
					this.connectToHost(newHost[0], fullPolicy);
				}
			} else if (discPolicy
					.equals(RpcFunctions.CONNECT_FAILED_POLICY_SITE)) {
				// connect to a host at the same site

				PlanetLabHost newHost = plcConnection
						.getRandomHostFromSite(host);
				if (newHost != null) {
					this.connectToHost(newHost, fullPolicy);
				} else {
					PlanetLabSite site = plcConnection.getSite(host);
					System.out.println("no more hosts available in site: "
							+ site.getName() + " (" + host.getHostname() + ")");
					PlanetLabHost[] hostFromOtherSite = plcConnection
							.getRandomSites(1);
					if (hostFromOtherSite.length > 0) {
						System.out
								.print("connected to a new random site instead");
						this.connectToHost(hostFromOtherSite[0], fullPolicy);
					}

				}
			} else if (discPolicy
					.equals(RpcFunctions.CONNECT_FAILED_POLICY_CLOSEST)) {
				// connect to the "closest" node
			}
		}
	}

	public int queueCommand(String command, double timeout, boolean stopOnError) {
		if (stopOnError) {
			System.out.println(command + ": stop on error");
		}
		synchronized (commandHistory) {
			for (Iterator iter = connections.iterator(); iter.hasNext();) {
				SshConnection conn = (SshConnection) iter.next();
				conn.queueCommand(command, timeout, stopOnError);

			}
			commandHistory.add(command);
		}

		return commandHistory.size() - 1;
	}

	public int killCommand(int commandId) {
		int killed = 0;
		synchronized (commandHistory) {
			for (Iterator iter = connections.iterator(); iter.hasNext();) {
				SshConnection conn = (SshConnection) iter.next();
				if (0 == conn.killCommand(commandId)) {
					killed++;
				}

			}
		}

		return killed;
	}

	// public int queueCommand(String command, int timeout) {
	// return this.queueCommand(command, 0, false);
	// }

	// public List<Map> readConsole(int index, int row) {
	// return connections.get(index).getOutput(row);
	// }

	public void shutdown() {
		connectThread.halt();
		this.disconnectAll();
	}

	public int download(String remotePath, String localPath, String fileFilter,
			String localNamingType) {

		StringBuffer command = new StringBuffer();
		command.append("#download ");
		command.append("\"" + remotePath + "\" ");
		command.append("\"" + localPath + "\" ");
		command.append("\"" + fileFilter + "\" ");
		command.append("\"" + localNamingType + "\"");
		this.queueCommand(command.toString(), 0, false);
		return 0;
	}

	/**
	 * Private class used to even out the rate at which new hosts are connected
	 * to
	 * 
	 * @author isdal
	 * 
	 */
	private class ConnectThread extends Thread {
		/**
		 * number of connects per second
		 * 
		 */
		private double CONNECT_PACE = 5.0;

		private LinkedBlockingQueue<SshConnection> hostQueue = new LinkedBlockingQueue<SshConnection>();

		private volatile boolean quit = false;

		public ConnectThread() {
			this.setName("ConnectThread");
			// if running on windows, decrease the connect rate to 2.0/s

			if (System.getProperty("os.name").toLowerCase().contains("windows")) {
				this.CONNECT_PACE = 2.0;
				System.out
						.println("running on windows, decreasing connect rate to 2 hosts/s");
			}

			// set deamon, meaning that the application will not wait for this
			// thread to complete if all other threads are completed
			this.setDaemon(true);
		}

		/**
		 * Adds the sshConnenction to the queue of connections to initate
		 * 
		 * @param sshConnection
		 *            the ssh connection
		 */
		public void connect(SshConnection sshConnection) {
			hostQueue.add(sshConnection);
		}

		/**
		 * stops the thread
		 * 
		 */
		public void halt() {
			quit = true;
			this.interrupt();
		}

		public void run() {
			while (!quit) {
				try {
					SshConnection conn = hostQueue.take();
					conn.start();
					Thread.sleep(Math.round(1000.0 / CONNECT_PACE));
				} catch (InterruptedException e) {
					// ignore this, probably means that we should quit
				}
			}
		}
	}

}
