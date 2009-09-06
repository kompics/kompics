package temporary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Semaphore;

import mine.ProcessLauncher;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.p2p.systems.bootstrap.server.BootstrapServerMain;
import se.sics.kompics.p2p.systems.chord.ChordPeerMain;
import se.sics.kompics.p2p.systems.chord.monitor.server.ChordMonitorServerMain;

public class LauncherTake1 {
	public static void main(String[] args) throws IOException,
			InterruptedException {
		// new Thread() {
		// public void run() {
		// try {
		// Configuration c = new Configuration(6501, true, false);
		// Process process1 = launch33(c.set(),
		// BootstrapServerMain.class.getCanonicalName());
		//
		// process1.waitFor();
		// } catch (IOException e) {
		// e.printStackTrace();
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		// };
		// }.start();
		//
		// new Thread() {
		// public void run() {
		// try {
		// Configuration c = new Configuration(6503, false, true);
		// Process process2 = launch33(c.set(),
		// ChordMonitorServerMain.class.getCanonicalName());
		// process2.waitFor();
		// } catch (IOException e) {
		// e.printStackTrace();
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		// };
		// }.start();

		launchProcess(BootstrapServerMain.class, 0, 7003);
		launchProcess(ChordMonitorServerMain.class, 0, 7001);

		Thread.sleep(1000);

		launchProcess(ChordPeerMain.class, 3, 6505);
		Thread.sleep(5000);
		launchProcess(ChordPeerMain.class, 100, 6507);

		System.err.println("TERMINATED");

		// launch(BootstrapServerMain.class, null);
	}

	private static void launchProcess(
			final Class<? extends ComponentDefinition> mainComponent,
			final int id, final int port2) {
		new Thread() {
			public void run() {
				try {
					Configuration c = new Configuration(port2, false, false);
					Process process2 = launch33(id, c.set(), mainComponent
							.getCanonicalName());
					process2.waitFor();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			};
		}.start();
	}

	@SuppressWarnings("unchecked")
	private static Process launch33(int id, Properties p, String mainClass)
			throws IOException {
		List<String> arguments = new ArrayList<String>();
		arguments.add("java");
		arguments.add("-classpath");
		arguments.add(System.getProperty("java.class.path"));

		Enumeration<String> keys = (Enumeration<String>) p.propertyNames();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			String value = p.getProperty(key);

			arguments.add("-D" + key + "=" + value);
		}

		arguments.add("-Dpeer.id=" + id);
		
		arguments.add(mainClass);

		ProcessBuilder pb = new ProcessBuilder(arguments);
		pb.redirectErrorStream(true);

		System.err.println(pb.command());

		Process process = pb.start();

		System.err.println("DONE");

		try {
			BufferedReader out = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
//			Writer input = new BufferedWriter(new OutputStreamWriter(process
//					.getOutputStream()));

			String line;
			do {
				line = out.readLine();
				if (line != null) {
					if (line.equals("2DIE")) {
						if (process != null) {
							process.destroy();
							process = null;
						}
						break;
					}
					System.out.println(line + "\n");
				}
			} while (line != null);
		} catch (Throwable e) {
			System.out.println(e.getMessage());
		}
		return process;
	}

	public static void launch(Class<? extends ComponentDefinition> c,
			Properties p) {
		int processCount = 1;

		Semaphore semaphore = new Semaphore(0, false);
		int idx = 1;

		ProcessLauncher processLauncher = new ProcessLauncher("", c.getName(),
				"-Dlog4j.properties=log4j.properties", 1, 0, idx, semaphore);
		// processes.put(pid, processLauncher);

		processLauncher.setProcessCount(processCount);
		processLauncher.start();

		// wait for all launchers to become ready
		semaphore.acquireUninterruptibly(processCount);

		System.out.println("DONE");
	}
}
