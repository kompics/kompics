package se.sics.kompics.wan.master.plab.rpc;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import se.sics.kompics.wan.master.plab.Credentials;
import se.sics.kompics.wan.master.plab.plc.PlanetLabHost;
import se.sics.kompics.wan.master.plab.plc.PlanetLabSite;
import se.sics.kompics.wan.master.ssh.CommandSpec;
import se.sics.kompics.wan.master.ssh.ConnectionController;
import se.sics.kompics.wan.master.ssh.SshConnection;


public class RpcServerFunctionsImpl implements RpcFunctions {

	public int addRandomHostsFromPLC(Map auth, int numberOfHosts) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return -1;
		}

		return controller.addRandomHostsFromPLC(numberOfHosts);
	}

	public int addRandomSitesFromPLC(Map auth, int numberOfSites) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return -1;
		}
		return controller.addRandomSitesFromPLC(numberOfSites);
	}

	public double commandCompleted(Map auth, int commandId) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return -1;
		}

		return controller.getCompletionStats(commandId);
	}

	public int connectToHost(Map credentials, String hostname) {

		System.out.println("connect: " + hostname);
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(credentials));
		if (controller == null) {
			return -1;
		}

		// check if there a PL host with this info
		PlanetLabHost host = controller.getHostInfo(hostname);
		if (host != null) {
			return controller.connectToHost(host, "");
		}
		// else, fall back on only hostname
		return controller.connectToHost(new PlanetLabHost(hostname), "");
	}

	public String getCommand(Map auth, int commandIndex) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return null;
		}
		return controller.getCommand(commandIndex);
	}

	public Object[] getCommandOverview(Map auth, int commandId) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return null;
		}
		Object[] obj = new Object[controller.getHostNum()];

		for (int i = 0; i < obj.length; i++) {
			Map<String, Object> map = new HashMap<String, Object>();
			PlanetLabHost host = controller.getPlanetLabHost(i);
			PlanetLabSite site = controller.getPlanetLabSite(host);
			SshConnection conn = controller.getSshConnection(i);
			CommandSpec spec = conn.getCommandSpec(commandId);

			map.put("connection_id", i);
			if (site != null) {
				map.put("site", site.getAbbreviated_name());
			} else {
				map.put("site", "");
			}
			if (host != null) {
				map.put("hostname", host.getHostname());
			} else {
				map.put("hostname", "");
			}
			if (conn != null) {
				map.put("connected", conn.isConnected());
				conn.getCommandSpec(commandId);
			} else {
				map.put("connected", "");
			}
			if (spec != null) {
				map.put("command", spec.getCommand());
				map.put("exit_code", spec.getExitCode());
				map.put("exit_code_string", spec.getExitCodeString());
				map.put("output_rows", spec.getLineNum());
				map.put("last_line", spec.getLastLine());
				map.put("exec_time", spec.getExecutionTime());
			} else {
				map.put("command", "");
				map.put("exit_code", -1);
				map.put("exit_code_string", "");
				map.put("output_rows", 0);
				map.put("last_line", "");
				map.put("exec_time", 0.0);
			}
			// System.out.println(site.getAbbreviated_name());
			obj[i] = map;
		}
		return obj;
	}

	public Object[] getCommandStats(Map auth, int connectionId) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return null;
		}
		return controller.getSshConnection(connectionId).getCommandStats();
	}

	public Object[] getCommandStatusOverview(Map auth) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return null;
		}

		Object[] obj = new Object[controller.getCommandNum()];

		for (int i = 0; i < obj.length; i++) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("command_index", i);
			map.put("command", controller.getCommand(i));
			map.put("percent_completed", controller.getCompletionStats(i));
			map.put("exit_code_counts", controller.getCommandExitStats(i));
			obj[i] = map;
		}

		return obj;
	}

	public Object[] getConnectedHosts(Map auth) {
		return this.getSuccessfulHosts(auth, 0);
	}

	public Map getExitStats(Map auth, int commandId) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return null;
		}
		return controller.getCommandExitStats(commandId);
	}

	public String getHostname(Map auth, int hostnum) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return "Access denied";
		}
		return controller.getSshConnection(hostnum).getHostname();
	}

	public Object[] getHostStatusOverview(Map auth) {

		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return null;
		}

		int totalHostNum = controller.getHostNum();

		Object[] obj = new Object[totalHostNum];
		// System.err.println("total commands: " + totalHostNum);
		for (int i = 0; i < totalHostNum; i++) {

			SshConnection conn = controller.getSshConnection(i);

			boolean connected = conn.isConnected();
			String hostname = conn.getHostname();
			int currentCommandNum = conn.getCurrentCommandNum();
			int completedCommandNum = currentCommandNum;
			int exitStatus = conn.getCommandExitStatus(currentCommandNum);
			String exitStatusString = conn
					.getCommandExitStatusString(currentCommandNum);

			String command = conn.getCommand(currentCommandNum);
			double executionTime = conn.getExecutionTime(currentCommandNum);
			boolean isCompleted = conn.isCommandCompleted(currentCommandNum);
			PlanetLabSite plSite = controller.getPlanetLabSite(conn.getHost());
			String site = null;
			if (plSite != null) {
				site = plSite.getAbbreviated_name();
			} else {
				site = "--non planetlab";
			}

			// add one if the current command is completed (except if the
			// current command is "connect" and the connect exit status is !=0)
			if (isCompleted && (currentCommandNum != 0 || exitStatus == 0)) {
				completedCommandNum++;
			}

			String lastLine = conn.getLastOutput();

			Map<String, Object> map = new HashMap<String, Object>();
			map.put("connected", connected);
			map.put("connection_id", i);
			map.put("site", site);
			map.put("hostname", hostname);
			map.put("current_command_num", currentCommandNum);
			map.put("completed_command_num", completedCommandNum);
			// map.put("exit_status", exitStatus);
			map.put("exit_stats_string", exitStatusString);
			map.put("command", command);
			// map.put("total_command_num", totalCommandNum);
			map.put("execution_time", executionTime);

			map.put("last_line", lastLine);
			obj[i] = map;
		}
		return obj;
	}

	public Object[] getSuccessfulHosts(Map auth, int commandId) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return new Object[0];
		}

		Vector<String> hosts = new Vector<String>();

		int hostnum = controller.getHostNum();
		for (int i = 0; i < hostnum; i++) {
			SshConnection conn = controller.getSshConnection(i);
			int exitCode = conn.getCommandExitStatus(commandId);
			if (exitCode == 0) {
				hosts.add(conn.getHostname());
			}
		}

		return hosts.toArray();
	}

	public int kill(Map auth, int commandId) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return -1;
		}

		return controller.killCommand(commandId);
	}

	public int queueCommand(Map credentials, String command) {
		return this.queueCommand(credentials, command, 0, false);
	}

	public int queueCommand(Map credentials, String command, double timeout) {
		return this.queueCommand(credentials, command, timeout, false);
	}

	public int queueCommand(Map credentials, String command, double timeout,
			boolean stopOnError) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(credentials));
		if (controller == null) {
			return -1;
		}
		return controller.queueCommand(command, timeout, stopOnError);
	}

	public int queueCommand(Map credentials, String command, double timeout,
			int stopOnError) {
		boolean stop = false;
		if (stopOnError > 0) {
			stop = true;
		}
		return this.queueCommand(credentials, command, timeout, stop);
	}

	public int queueCommand(Map credentials, String command, int timeout) {
		return this.queueCommand(credentials, command, timeout, false);
	}

	public Object[] readConsole(Map auth, int connectionId, int fromRow) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return null;
		}
		SshConnection conn = controller.getSshConnection(connectionId);
		List<Map> output = conn.getOutput(fromRow);
		// System.out.println("SERVER>reading from " +fromRow +" to " + (fromRow
		// + output.size()));
		return output.toArray();
	}

	public int shutdown(Map auth) {
		return RpcServer.getInstance().shutdown(new Credentials(auth));
	}

	public int totalCommandNum(Map auth) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return -1;
		}

		return controller.getCommandNum();
	}

	public int totalHostNum(Map auth) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return -1;
		}

		return controller.getHostNum();
	}

	public int upload(Map auth, String path, double timeout, boolean stopOnError) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return -1;
		}
		File testFile = new File(path);
		if (testFile.exists()) {
			this.queueCommand(auth, RpcFunctions.SPECIAL_COMMAND_UPLOAD_DIR
					+ " \"" + path + "\"", timeout, stopOnError);
			return 0;
		}
		return 1;
	}

	public int upload(Map auth, String path, double timeout, int stopOnError) {
		boolean stop = false;
		if (stopOnError > 0) {
			stop = true;
		}
		return this.upload(auth, path, timeout, stop);
	}

	public int download(Map auth, String remotePath, String localPath,
			String fileFilter, String localNamingType) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return -1;
		}
		return controller.download(remotePath, localPath, fileFilter,
				localNamingType);
	}

	public int addHostsToSlice(Map auth, Object[] hosts) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return -1;
		}

		String[] h = new String[hosts.length];
		for (int i = 0; i < hosts.length; i++) {
			h[i] = (String) hosts[i];
		}
		return controller.addHostsToSlice(h);
	}

	public Object[] getHostsNotInSlice(Map auth) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return null;
		}

		ArrayList<Integer> list = new ArrayList<Integer>();
		PlanetLabHost[] hosts = controller.getHostsNotInSlice();
		for (int i = 0; i < hosts.length; i++) {
			if (hosts[i].getIp() != null) {
				list.add(hosts[i].getNode_id());
			}
		}
		return list.toArray(new Object[list.size()]);
	}

	public int fetchCoMonData(Map auth) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return -1;
		}
		controller.getCoMonData();
		return 0;
	}

	public double getCoMonProgress(Map auth) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return -1;
		}
		return controller.getCoMonProgress();
	}

	public Object[] getHostStats(Map auth, Object[] hosts) {
		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return new Object[0];
		}

		Object[] result = new Object[hosts.length];
		// System.out.println(hosts.length);
		for (int i = 0; i < hosts.length; i++) {
			Integer host = (Integer) hosts[i];
			PlanetLabHost plHost = controller.getHostInfo(host);
			Map<String, String> map = new HashMap<String, String>();

			map.put("hostname", plHost.getHostname());
			map.put("node_id", "" + plHost.getNode_id());
			map.put("site", controller.getPlanetLabSite(plHost)
					.getAbbreviated_name());
			if (plHost.getComMonStat() != null) {
				map.put("load_average", ""
						+ plHost.getComMonStat().getLoadAverage());
				map.put("response_time", ""
						+ plHost.getComMonStat().getResponseTime());
				map.put("mem_free", "" + plHost.getComMonStat().getMemFree());
				map.put("mem_total", "" + plHost.getComMonStat().getMemTotal());
			}
			result[i] = map;
		}

		return result;
	}

	public Object[] getAllHosts(Map auth) {

		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return null;
		}

		ArrayList<Integer> list = new ArrayList<Integer>();
		PlanetLabHost[] hosts = controller.getAllHosts();
		for (int i = 0; i < hosts.length; i++) {
			if (hosts[i].getIp() != null) {
				list.add(hosts[i].getNode_id());
				// System.out.println("adding:" + hosts[i].getHostname());
			} else {
				System.out.println("node_id=null: " + hosts[i].getHostname());
			}
		}

		return list.toArray(new Object[list.size()]);
	}

	public Object[] getAvailableHosts(Map auth) {

		ConnectionController controller = RpcServer.getInstance()
				.getController(new Credentials(auth));
		if (controller == null) {
			return null;
		}

		ArrayList<Integer> list = new ArrayList<Integer>();
		PlanetLabHost[] hosts = controller.getAvailableHosts();
		for (int i = 0; i < hosts.length; i++) {
			if (hosts[i].getIp() != null) {
				list.add(hosts[i].getNode_id());
				// System.out.println("adding:" + hosts[i].getHostname());
			} else {
				System.out.println("node_id=null: " + hosts[i].getHostname());
			}
		}

		return list.toArray(new Object[list.size()]);
	}

	
}
