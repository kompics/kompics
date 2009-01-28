/**
 * This file is part of the Kompics component model runtime.
 * 
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics.launch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * The <code>ProcessLauncher</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id: ProcessLauncher.java 268 2008-09-28 19:18:04Z Cosmin $
 */
public final class ProcessLauncher implements Runnable {

	private final String classpath;
	private final String mainClass;
	private final String log4jProperties;
	private final int id;
	private final String command;

	private Process process;
	private int processCount;
	private ProcessOutputFrame mainFrame;
	private BufferedWriter input;
	private Scenario launcher;

	/**
	 * Instantiates a new process launcher.
	 * 
	 * @param classpath
	 *            the classpath
	 * @param mainClass
	 *            the main class
	 * @param log4jProperties
	 *            the log4j properties
	 * @param command
	 *            the command
	 * @param id
	 *            the id
	 * @param launcher
	 *            the launcher
	 */
	public ProcessLauncher(String classpath, String mainClass,
			String log4jProperties, String command, int id, Scenario launcher) {
		this.classpath = classpath;
		this.mainClass = mainClass;
		this.log4jProperties = log4jProperties;
		this.command = command;
		this.id = id;
		this.launcher = launcher;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		mainFrame = new ProcessOutputFrame(this, command, "" + id,
				processCount, launcher);
		mainFrame.setVisible(true);

		ProcessBuilder processBuilder = new ProcessBuilder("java",
				"-classpath", classpath, log4jProperties, "-Dtopology="
						+ topologyFile, mainClass, "" + id, command);
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

		mainFrame.append("Process " + id + " has terminated.");
	}

	/**
	 * Kill.
	 * 
	 * @param dispose
	 *            the dispose
	 */
	public void kill(boolean dispose) {
		if (process != null) {
			process.destroy();
			if (dispose)
				mainFrame.dispose();
		}
	}

	/**
	 * Input.
	 * 
	 * @param string
	 *            the string
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void input(String string) throws IOException {
		input.write(string);
		input.write("\n");
		input.flush();
	}

	/**
	 * Sets the process count.
	 * 
	 * @param processCount
	 *            the new process count
	 */
	public void setProcessCount(int processCount) {
		this.processCount = processCount;
	}

	private String topologyFile;

	/**
	 * Sets the topology file.
	 * 
	 * @param topologyFile
	 *            the new topology file
	 */
	public void setTopologyFile(String topologyFile) {
		this.topologyFile = topologyFile;
	}

	/**
	 * Wait for.
	 */
	public void waitFor() {
		if (process != null) {
			while (true) {
				try {
					process.waitFor();
					break;
				} catch (InterruptedException e) {
					continue;
				}
			}
		}
	}
}
