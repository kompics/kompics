package se.sics.kompics.p2p;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>P2pLauncher</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
public final class P2pLauncher {

	static {
		PropertyConfigurator.configureAndWatch("log4j.properties");
	}

	private static final Logger logger = LoggerFactory
			.getLogger(P2pLauncher.class);

	private static String classPath = ManagementFactory.getRuntimeMXBean()
			.getClassPath();

	public static ProcessLauncher processes[];

	/**
	 * Launches protocol application processes according to a global
	 * configuration comprised by a network topology and a set of sequences of
	 * application commands, one sequence per process.
	 * 
	 * @param args
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException,
			IOException {

		System.out.println(classPath);

		Properties commands = new Properties();
		commands.load(P2pLauncher.class
				.getResourceAsStream("p2p-launcher.properties"));

		boolean launchBootrapServer = commands.getProperty(
				"launch.bootstrap.server", "no").equals("yes");
		boolean launchMonitorServer = commands.getProperty(
				"launch.monitor.server", "no").equals("yes");
		int nodes = Integer.parseInt(commands.getProperty("launch.nodes"));

		processes = new ProcessLauncher[nodes + 2];

		if (launchBootrapServer) {
			// start Bootstrap server
			processes[1] = launchProcess(
					"se.sics.kompics.p2p.BootstrapServerMain",
					"-Dlog4j.properties=log4j.properties", "Bootstrap Server",
					1);

			logger.debug("Launched Bootrap Server");
		}
		if (launchMonitorServer) {
			// start PeerMonitorServer
			processes[0] = launchProcess(
					"se.sics.kompics.p2p.PeerMonitorServerMain",
					"-Dlog4j.properties=log4j.properties",
					"P2P Monitor Server", 0);

			logger.debug("Launched P2P Monitor Server");
		}

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		for (int i = 1; i <= nodes; i++) {
			String nodeCommand = commands.getProperty(i + ".command");
			String nodeNetworkAddress = commands.getProperty(i + ".network");
			String nodeWebAddress = commands.getProperty(i + ".web");
			if (nodeCommand != null) {
				processes[i + 1] = launchProcess("se.sics.kompics.p2p.P2pMain "
						+ nodeNetworkAddress + " " + nodeWebAddress + " "
						+ nodeCommand,
						"-Xmx128m -Dlog4j.properties=log4j.properties", "Node "
								+ i, i + 1);

				logger.debug("Launched Node {}", i);
			}
		}
	}

	private static ProcessLauncher launchProcess(String command,
			String switches, String name, int pid) throws IOException {

		ProcessLauncher processLauncher = new ProcessLauncher(classPath,
				switches, command, name, pid);
		processLauncher.start();
		return processLauncher;
	}

	public static void killAll() {
		for (int i = 0; i < processes.length; i++) {
			if (processes[i] != null) {
				processes[i].kill(true);
			}
		}
		System.exit(0);
	}
}
