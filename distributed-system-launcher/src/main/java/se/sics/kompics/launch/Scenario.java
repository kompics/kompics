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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import se.sics.kompics.ComponentDefinition;

/**
 * The <code>Scenario</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id: Scenario.java 268 2008-09-28 19:18:04Z Cosmin $
 */
public abstract class Scenario {

	private final String classPath = System.getProperty("java.class.path");
	private final String mainClass;
	private final String name;

	private HashMap<Integer, ProcessLauncher> processes = new HashMap<Integer, ProcessLauncher>();
	private int processCount = 0;

	/**
	 * Instantiates a new scenario.
	 * 
	 * @param mainClass
	 *            the main class
	 */
	protected Scenario(Class<? extends ComponentDefinition> mainClass) {
		this.mainClass = mainClass.getCanonicalName();
		this.name = mainClass.getSimpleName();
	}

	/**
	 * Command.
	 * 
	 * @param pid
	 *            the pid
	 * @param command
	 *            the command
	 */
	protected final void command(int pid, String command) {
		ProcessLauncher processLauncher = createProcess(pid, command);
		processes.put(pid, processLauncher);
		processCount++;
	}

	/**
	 * Execute on fully connected.
	 * 
	 * @param topology
	 *            the topology
	 */
	public void executeOnFullyConnected(Topology topology) {
		topology.checkFullyConnected();
		executeOn(topology);
	}

	/**
	 * Execute on.
	 * 
	 * @param topology
	 *            the topology
	 */
	public final void executeOn(Topology topology) {
		if (processCount > topology.getNodeCount()) {
			throw new RuntimeException(
					"Scenario has more processes than topology has nodes");
		}
		if (processCount < topology.getNodeCount()) {
			System.err.println("Warning: Some topology nodes unused");
		}

		System.out.print("Executiong " + name + "... ");
		File file = null;
		try {
			file = File.createTempFile("topology", ".bin");
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(file));
			oos.writeObject(topology);
			oos.flush();
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		for (ProcessLauncher processLauncher : processes.values()) {
			processLauncher.setProcessCount(processCount);
			processLauncher.setTopologyFile(file.getAbsolutePath());
			new Thread(processLauncher).start();
		}

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		for (ProcessLauncher processLauncher : processes.values()) {
			processLauncher.waitFor();
		}
		System.out.println("done.");
	}

	/**
	 * Kill all.
	 */
	public final void killAll() {
		for (ProcessLauncher processLauncher : processes.values()) {
			processLauncher.kill(true);
		}
	}

	private ProcessLauncher createProcess(int id, String command) {
		ProcessLauncher processLauncher = new ProcessLauncher(classPath,
				mainClass, "-Dlog4j.properties=log4j.properties", (command
						.equals("") ? " " : command), id, this);
		return processLauncher;
	}
}
