package se.sics.kompics.p2p;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JTextArea;

/**
 * The <code>ProcessLauncher</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: ProcessLauncher.java 76 2008-05-14 12:11:14Z cosmin $
 */
public class ProcessLauncher extends Thread {

	private String classpath;

	private String properties;

	private String command;

	private String name;

	private Process process;

	private int pid;

	ProcessOutputFrame mainFrame;

	public ProcessLauncher(String classpath, String properties, String command,
			String name, int pid) {
		this.classpath = classpath;
		this.properties = properties;
		this.command = command;
		this.name = name;
		this.pid = pid;
	}

	public void run() {
		String arguments[] = command.split(" ");
		mainFrame = new ProcessOutputFrame(this,
				arguments[arguments.length - 1], name, pid);
		mainFrame.setVisible(true);
		JTextArea logArea = mainFrame.getLogArea();

		List<String> commandArgs = new LinkedList<String>();
		commandArgs.add("java");
		commandArgs.add("-D " + name);
		// commandArgs.add("-Dcom.sun.management.jmxremote");
		// commandArgs.add("-Xrunjmeter");
		// commandArgs.add("-Xbootclasspath/a:$JMETER_HOME/lib/HPjmeter.jar");
		// commandArgs.add("-agentlib:jmeter");
		// if (name.equals("Node 1")) {
		// commandArgs.add("-Xrunhprof:cpu=samples");
		// }

		if (name.equals("Node 1")) {
			commandArgs.add("-agentlib:jprofilerti=port=8849,nowait,id=105,"
					+ "config=C:\\Users\\Cosmin\\.jprofiler5\\config.xml");
			commandArgs.add("\"-Xbootclasspath/a:"
					+ "C:\\Program Files (x86)\\jprofiler5\\bin\\agent.jar\"");
		}

		commandArgs.add("-classpath");
		commandArgs.add(classpath);
		String switches[] = properties.split(" ");
		for (int i = 0; i < switches.length; i++) {
			commandArgs.add(switches[i]);
		}
		for (int i = 0; i < arguments.length; i++) {
			commandArgs.add(arguments[i]);
		}

		ProcessBuilder processBuilder = new ProcessBuilder(commandArgs);
		processBuilder.redirectErrorStream(true);

		try {
			process = processBuilder.start();

			BufferedReader out = new BufferedReader(new InputStreamReader(
					process.getInputStream()));

			String line;
			do {
				line = out.readLine();
				if (line != null) {
					logArea.append(line + "\n");
				}
			} while (line != null);
		} catch (IOException e) {
			logArea.append(e.getMessage());
		}

		logArea.append("Process has terminated.");
	}

	public void kill(boolean dispose) {
		if (process != null) {
			process.destroy();
			if (dispose)
				mainFrame.dispose();
		}
	}
}
