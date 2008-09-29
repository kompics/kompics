package se.sics.kompics.p2p;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

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

	private BufferedWriter input;

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

		List<String> commandArgs = new LinkedList<String>();
		commandArgs.add("java");
		commandArgs.add("-D " + name);

		commandArgs.add("-Xmx1g");

//		commandArgs.add("-Dcom.sun.management.jmxremote");

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
			input = new BufferedWriter(new OutputStreamWriter(process
					.getOutputStream()));

			String line;
			do {
				line = out.readLine();
				if (line != null) {
					mainFrame.append(line + "\n");
				}
			} while (line != null);
		} catch (IOException e) {
			mainFrame.append(e.getMessage());
		}

		mainFrame.append("Process has terminated.");
	}

	public void kill(boolean dispose) {
		if (process != null) {
			process.destroy();
			process = null;
			if (dispose)
				mainFrame.dispose();
		}
	}

	public void input(String string) throws IOException {
		input.write(string);
		input.write("\n");
		input.flush();
	}
}
