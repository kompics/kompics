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
package mine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The <code>ProcessLauncher</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id: ProcessLauncher.java 734 2009-03-11 17:02:19Z Cosmin $
 */
public final class ProcessLauncher extends Thread {

	private final String classpath;
	private final String mainClass;
	private final String log4jProperties;
	private final int id;
	private final int idx;
	private final Semaphore semaphore;
	private final AtomicBoolean busy;

	private Process process;
	private int processCount;
	private ProcessFrame mainFrame;
	private BufferedWriter input;
	private final long startedAt;

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
			String log4jProperties, int id, int idx, long now,
			Semaphore semaphore) {
		this.classpath = classpath;
		this.mainClass = mainClass;
		this.log4jProperties = log4jProperties;
		this.id = id;
		this.idx = idx;
		this.semaphore = semaphore;
		this.startedAt = now;
		this.busy = new AtomicBoolean(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public final void run() {
//		mainFrame = new ProcessFrame(this, "", "" + id, idx, processCount);
		launchProcess("null", "started");
		waitFor();
		semaphore.release();
		busy.set(false);
	}

	public final void recover(String script) {
		boolean success = busy.compareAndSet(false, true);
		if (success) {
			semaphore.acquireUninterruptibly();
			launchProcess(script, "recovered");
			waitFor();
			semaphore.release();
			busy.set(false);
		} else {
			System.err.println("Process " + id + " is active.");
		}
	}

	private void launchProcess(String commandScript, String action) {
		if (commandScript.equals("")) {
			// we add an empty command if command is void
			commandScript = " ";
		}
		mainFrame.setTitle("Process " + id + " - " + commandScript);
		mainFrame.setVisible(true);

		ProcessBuilder processBuilder = new ProcessBuilder("java",
				"-classpath", classpath, log4jProperties, "-Dtopology="
						+ topologyFile, mainClass, "" + id, commandScript);
		processBuilder.redirectErrorStream(true);

		try {
			process = processBuilder.start();
			BufferedReader out = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			input = new BufferedWriter(new OutputStreamWriter(process
					.getOutputStream()));

			String log = String.format(
					"%5d@SCENARIO {%s} Process %d has %s commands [%s].\n",
					now(), " sss ", id, action, commandScript);
			mainFrame.append(log);
			System.out.print(log);

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
					mainFrame.append(line + "\n");
				}
			} while (line != null);
		} catch (Throwable e) {
			mainFrame.append(e.getMessage());
		}

		String log = String.format(
				"%5d@SCENARIO {%s} Process %d has terminated.\n", now(),
				"sssh", id);
		mainFrame.append(log);
		System.out.print(log);
	}

	/**
	 * Kill.
	 * 
	 * @param dispose
	 *            the dispose
	 */
	public final void kill(boolean dispose) {
		if (process != null) {
			process.destroy();
			process = null;
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
	final void input(String string) throws IOException {
		input.write(string);
		input.write("\n");
		input.flush();
	}

	final void globalInput(String string) throws IOException {
//		scenario.globalInput(string);
	}

	final void handleInput(String string) throws IOException {
		if (process != null) {
			input.write(string);
			input.write("\n");
			input.flush();
			mainFrame.append(string + "\n");
		}
	}

	/**
	 * Sets the process count.
	 * 
	 * @param processCount
	 *            the new process count
	 */
	public final void setProcessCount(int processCount) {
		this.processCount = processCount;
	}

	private String topologyFile;

	/**
	 * Sets the topology file.
	 * 
	 * @param topologyFile
	 *            the new topology file
	 */
	public final void setTopologyFile(String topologyFile) {
		this.topologyFile = topologyFile;
	}

	/**
	 * Wait for.
	 */
	private final void waitFor() {
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

	private final long now() {
		return System.currentTimeMillis() - startedAt;
	}
}
