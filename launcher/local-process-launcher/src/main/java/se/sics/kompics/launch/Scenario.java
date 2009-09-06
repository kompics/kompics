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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import se.sics.kompics.ComponentDefinition;

/**
 * The <code>Scenario</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
public abstract class Scenario {

	protected static final class Command {
		protected final String command;
		protected final long delayMs;
		protected Command nextCommand;

		private Command(String command) {
			this(command, 0);
		}

		private Command(String command, long delayMs) {
			this.command = command;
			this.delayMs = delayMs;
			this.nextCommand = null;
		}

		public final Command recover(long delayMs) {
			nextCommand = new Command(command, delayMs);
			return nextCommand;
		}

		public final Command recover(String command, long delayMs) {
			nextCommand = new Command(command, delayMs);
			return nextCommand;
		}
	}

	private final String classPath = System.getProperty("java.class.path");
	private final String mainClass;
	final String name;

	private HashMap<Integer, OldProcessLauncher> processes = new HashMap<Integer, OldProcessLauncher>();
	private HashMap<Integer, Command> commands = new HashMap<Integer, Command>();
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
	 *            the process id
	 * @param cmd
	 *            the command
	 */
	protected final Command command(int pid, String cmd) {
		return command(pid, cmd, 0);
	}

	/**
	 * Command.
	 * 
	 * @param pid
	 *            the process id
	 * @param command
	 *            the cmd
	 * @param command
	 *            the delay in milliseconds after which to start the process
	 */
	protected final Command command(int pid, String cmd, long delayMs) {
		Command command = new Command(cmd, delayMs);
		Command oldCommand = commands.put(pid, command);
		if (oldCommand != null) {
			throw new RuntimeException(
					"Cannot define two command scripts for process " + pid);
		}

		processCount++;
		return command;
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

		System.out.println("Executing " + name + "... ");

		// write the topology to file
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

		long now = System.currentTimeMillis();
		Semaphore semaphore = new Semaphore(0, false);
		int idx = 1;

		// create process launchers
		for (int pid : commands.keySet()) {
			Command command = commands.get(pid);
			OldProcessLauncher processLauncher = createProcess(pid, idx++,
					command, now, semaphore);
			processes.put(pid, processLauncher);

			processLauncher.setProcessCount(processCount);
			processLauncher.setTopologyFile(file.getAbsolutePath());
			processLauncher.start();
		}

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}

		startInputReaderThread();

		// wait for all launchers to become ready
		semaphore.acquireUninterruptibly(processCount);

		System.out.println("DONE");
	}

	/**
	 * Kill all.
	 */
	public final void killAll() {
		for (OldProcessLauncher processLauncher : processes.values()) {
			processLauncher.kill(true);
		}
	}

	/**
	 * Kill.
	 */
	public final void killNode(int id) {
		OldProcessLauncher processLauncher = processes.get(id);
		if (processLauncher != null) {
			processLauncher.kill(false);
		}
	}

	private OldProcessLauncher createProcess(int id, int idx, Command command,
			long now, Semaphore semaphore) {
		OldProcessLauncher processLauncher = new OldProcessLauncher(classPath,
				mainClass, "-Dlog4j.properties=log4j.properties", command, id,
				idx, this, now, semaphore);
		return processLauncher;
	}

	private void tryRecover(int id, final String command) {
		final OldProcessLauncher processLauncher = processes.get(id);
		if (processLauncher == null) {
			System.err.println("Scenario does not contain process " + id);
			return;
		}
		(new Thread("Process Recovery Launcher") {
			public void run() {
				processLauncher.recover(command);
			}
		}).start();
	}

	final void globalInput(String string) throws IOException {
		for (OldProcessLauncher processLauncher : processes.values()) {
			processLauncher.handleInput(string);
		}
	}

	private void startInputReaderThread() {
		Thread inputReader = new Thread("ScenarioInputReader") {
			public void run() {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						System.in));
				while (true) {
					try {
						String line = in.readLine();

						if (line.startsWith("recover")) {
							String[] args = line.substring(8).split("@");
							if (args.length == 2) {
								try {
									int id = Integer.parseInt(args[0]);
									String command = args[1];
									tryRecover(id, command);
									continue;
								} catch (RuntimeException e) {
								}
							}
						}
						System.out.println("Try 'recover@pid@command'");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		};
		inputReader.setDaemon(true);
		inputReader.start();
		System.out.println("For process recovery try 'recover@pid@command'"
				+ ", as in 'recover@1@S100:help'");
	}
}
