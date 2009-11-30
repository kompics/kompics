/**
 * This file is part of the Kompics P2P Framework.
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
package se.sics.kompics.p2p.experiment.dsl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Random;
import java.util.TimeZone;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.Loader;
import javassist.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Event;
import se.sics.kompics.p2p.experiment.dsl.adaptor.ConcreteOperation;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation1;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation2;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation3;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation4;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation5;
import se.sics.kompics.p2p.experiment.dsl.adaptor.OperationGenerator;
import se.sics.kompics.p2p.experiment.dsl.distribution.BigIntegerExponentialDistribution;
import se.sics.kompics.p2p.experiment.dsl.distribution.BigIntegerNormalDistribution;
import se.sics.kompics.p2p.experiment.dsl.distribution.BigIntegerUniformDistribution;
import se.sics.kompics.p2p.experiment.dsl.distribution.ConstantDistribution;
import se.sics.kompics.p2p.experiment.dsl.distribution.Distribution;
import se.sics.kompics.p2p.experiment.dsl.distribution.DoubleExponentialDistribution;
import se.sics.kompics.p2p.experiment.dsl.distribution.DoubleNormalDistribution;
import se.sics.kompics.p2p.experiment.dsl.distribution.DoubleUniformDistribution;
import se.sics.kompics.p2p.experiment.dsl.distribution.LongExponentialDistribution;
import se.sics.kompics.p2p.experiment.dsl.distribution.LongNormalDistribution;
import se.sics.kompics.p2p.experiment.dsl.distribution.LongUniformDistribution;
import se.sics.kompics.p2p.experiment.dsl.events.SimulationTerminatedEvent;
import se.sics.kompics.p2p.experiment.dsl.events.SimulatorEvent;
import se.sics.kompics.p2p.experiment.dsl.events.StochasticProcessEvent;
import se.sics.kompics.p2p.experiment.dsl.events.StochasticProcessStartEvent;
import se.sics.kompics.p2p.experiment.dsl.events.StochasticProcessTerminatedEvent;
import se.sics.kompics.p2p.experiment.dsl.events.TakeSnapshot;
import se.sics.kompics.p2p.experiment.dsl.events.TakeSnapshotEvent;
import se.sics.kompics.simulation.TimeInterceptor;

/**
 * The <code>SimulationScenario</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public abstract class SimulationScenario implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5278102582431240537L;

	private static Logger logger = LoggerFactory
			.getLogger("se.sics.kompics.simulation.CodeInstrumenter");

	private final Random random;
	private final LinkedList<StochasticProcess> processes;
	private int processCount;
	private SimulationTerminatedEvent terminatedEvent;

	public SimulationScenario() {
		this.random = new Random();
		this.processes = new LinkedList<StochasticProcess>();
		this.processCount = 0;
	}

	public void setSeed(long seed) {
		random.setSeed(seed);
	}

	public Random getRandom() {
		return random;
	}

	protected abstract class StochasticProcess implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = -6303689523381305745L;
		private boolean relativeStartTime;
		private long startTime;
		private StochasticProcessStartEvent startEvent;
		private StochasticProcessTerminatedEvent terminateEvent;
		private StochasticProcessEvent stochasticEvent;
		private Distribution<Long> interArrivalTime = null;
		private LinkedList<OperationGenerator> generators = new LinkedList<OperationGenerator>();
		private final String name;
		private boolean started = false;

		protected StochasticProcess(String name) {
			this.name = name;
			processCount++;
		}

		protected StochasticProcess() {
			processCount++;
			this.name = "Process" + processCount;
		}

		protected final void eventInterArrivalTime(
				Distribution<Long> interArrivalTime) {
			this.interArrivalTime = interArrivalTime;
		}

		protected final <E extends Event> void raise(int count, Operation<E> op) {
			if (count <= 0) {
				throw new RuntimeException(
						"Number of raised events must be strictly positive");
			}
			OperationGenerator generator = new OperationGenerator(
					new ConcreteOperation<E, Number, Number, Number, Number, Number>(
							op), count);
			generators.add(generator);
		}

		protected final <E extends Event, P1 extends Number> void raise(
				int count, Operation1<E, P1> op1, Distribution<P1> d1) {
			if (count <= 0) {
				throw new RuntimeException(
						"Number of raised events must be strictly positive");
			}
			OperationGenerator generator = new OperationGenerator(
					new ConcreteOperation<E, P1, Number, Number, Number, Number>(
							op1, d1), count);
			generators.add(generator);
		}

		protected final <E extends Event, P1 extends Number, P2 extends Number> void raise(
				int count, Operation2<E, P1, P2> op2, Distribution<P1> d1,
				Distribution<P2> d2) {
			if (count <= 0) {
				throw new RuntimeException(
						"Number of raised events must be strictly positive");
			}
			OperationGenerator generator = new OperationGenerator(
					new ConcreteOperation<E, P1, P2, Number, Number, Number>(
							op2, d1, d2), count);
			generators.add(generator);
		}

		protected final <E extends Event, P1 extends Number, P2 extends Number, P3 extends Number> void raise(
				int count, Operation3<E, P1, P2, P3> op3, Distribution<P1> d1,
				Distribution<P2> d2, Distribution<P3> d3) {
			if (count <= 0) {
				throw new RuntimeException(
						"Number of raised events must be strictly positive");
			}
			OperationGenerator generator = new OperationGenerator(
					new ConcreteOperation<E, P1, P2, P3, Number, Number>(op3,
							d1, d2, d3), count);
			generators.add(generator);
		}

		protected final <E extends Event, P1 extends Number, P2 extends Number, P3 extends Number, P4 extends Number, P5 extends Number> void raise(
				int count, Operation4<E, P1, P2, P3, P4> op4,
				Distribution<P1> d1, Distribution<P2> d2, Distribution<P3> d3,
				Distribution<P4> d4) {
			if (count <= 0) {
				throw new RuntimeException(
						"Number of raised events must be strictly positive");
			}
			OperationGenerator generator = new OperationGenerator(
					new ConcreteOperation<E, P1, P2, P3, P4, Number>(op4, d1,
							d2, d3, d4), count);
			generators.add(generator);
		}

		protected final <E extends Event, P1 extends Number, P2 extends Number, P3 extends Number, P4 extends Number, P5 extends Number> void raise(
				int count, Operation5<E, P1, P2, P3, P4, P5> op5,
				Distribution<P1> d1, Distribution<P2> d2, Distribution<P3> d3,
				Distribution<P4> d4, Distribution<P5> d5) {
			if (count <= 0) {
				throw new RuntimeException(
						"Number of raised events must be strictly positive");
			}
			OperationGenerator generator = new OperationGenerator(
					new ConcreteOperation<E, P1, P2, P3, P4, P5>(op5, d1, d2,
							d3, d4, d5), count);
			generators.add(generator);
		}

		public final void start() {
			relativeStartTime = false;
			startTime = 0;
			started = true;
			terminateEvent = new StochasticProcessTerminatedEvent(0,
					new LinkedList<StochasticProcessStartEvent>(), name);
			stochasticEvent = new StochasticProcessEvent(0, interArrivalTime,
					terminateEvent, generators, name);
			startEvent = new StochasticProcessStartEvent(startTime,
					new LinkedList<StochasticProcessStartEvent>(),
					stochasticEvent, 0, name);

			processes.remove(this);
			processes.add(this);
		}

		public final void startAt(long time) {
			relativeStartTime = false;
			startTime = time;
			started = true;
			terminateEvent = new StochasticProcessTerminatedEvent(0,
					new LinkedList<StochasticProcessStartEvent>(), name);
			stochasticEvent = new StochasticProcessEvent(0, interArrivalTime,
					terminateEvent, generators, name);
			startEvent = new StochasticProcessStartEvent(startTime,
					new LinkedList<StochasticProcessStartEvent>(),
					stochasticEvent, 0, name);

			processes.remove(this);
			processes.add(this);
		}

		public final void startAtSameTimeWith(StochasticProcess process) {
			relativeStartTime = true;
			started = true;
			startTime = 0;
			terminateEvent = new StochasticProcessTerminatedEvent(0,
					new LinkedList<StochasticProcessStartEvent>(), name);
			stochasticEvent = new StochasticProcessEvent(0, interArrivalTime,
					terminateEvent, generators, name);
			startEvent = new StochasticProcessStartEvent(startTime,
					new LinkedList<StochasticProcessStartEvent>(),
					stochasticEvent, 0, name);
			// we hook this process' start event to the referenced process'
			// list of start events
			if (!process.started) {
				throw new RuntimeException(process.name + " not started");
			}
			process.startEvent.getStartEvents().add(startEvent);

			processes.remove(this);
			processes.add(this);
		}

		public final void startAfterStartOf(long delay,
				StochasticProcess process) {
			relativeStartTime = true;
			started = true;
			startTime = delay;
			terminateEvent = new StochasticProcessTerminatedEvent(0,
					new LinkedList<StochasticProcessStartEvent>(), name);
			stochasticEvent = new StochasticProcessEvent(0, interArrivalTime,
					terminateEvent, generators, name);
			startEvent = new StochasticProcessStartEvent(startTime,
					new LinkedList<StochasticProcessStartEvent>(),
					stochasticEvent, 0, name);
			// we hook this process' start event to the referenced process'
			// list of start events
			if (!process.started) {
				throw new RuntimeException(process.name + " not started");
			}
			process.startEvent.getStartEvents().add(startEvent);

			processes.remove(this);
			processes.add(this);
		}

		public final void startAfterTerminationOf(long delay,
				StochasticProcess... process) {
			relativeStartTime = true;
			started = true;
			startTime = delay;
			terminateEvent = new StochasticProcessTerminatedEvent(0,
					new LinkedList<StochasticProcessStartEvent>(), name);
			stochasticEvent = new StochasticProcessEvent(0, interArrivalTime,
					terminateEvent, generators, name);
			startEvent = new StochasticProcessStartEvent(startTime,
					new LinkedList<StochasticProcessStartEvent>(),
					stochasticEvent, process.length, name);
			// we hook this process' start event to the referenced process'
			// list of start events
			HashSet<StochasticProcess> procs = new HashSet<StochasticProcess>(
					Arrays.asList(process));
			for (StochasticProcess stochasticProcess : procs) {
				if (!stochasticProcess.started) {
					throw new RuntimeException(stochasticProcess.name
							+ " not started");
				}
				stochasticProcess.terminateEvent.getStartEvents().add(
						startEvent);
			}

			processes.remove(this);
			processes.add(this);
		}
	}

	protected final void terminateAt(long time) {
		SimulationTerminatedEvent terminationEvent = new SimulationTerminatedEvent(
				time, 0, false);
		terminatedEvent = terminationEvent;
	}

	protected final void terminateAfterTerminationOf(long delay,
			StochasticProcess... process) {
		HashSet<StochasticProcess> procs = new HashSet<StochasticProcess>(
				Arrays.asList(process));

		SimulationTerminatedEvent terminationEvent = new SimulationTerminatedEvent(
				delay, procs.size(), true);
		terminatedEvent = terminationEvent;
		for (StochasticProcess stochasticProcess : procs) {
			if (!stochasticProcess.started) {
				throw new RuntimeException(stochasticProcess.name
						+ " not started");
			}
			stochasticProcess.terminateEvent
					.setTerminationEvent(terminationEvent);
		}
	}

	protected final Distribution<Double> constant(double value) {
		return new ConstantDistribution<Double>(Double.class, value);
	}

	protected final Distribution<Long> constant(long value) {
		return new ConstantDistribution<Long>(Long.class, value);
	}

	protected final Distribution<BigInteger> constant(BigInteger value) {
		return new ConstantDistribution<BigInteger>(BigInteger.class, value);
	}

	protected final Distribution<Double> uniform(double min, double max) {
		return new DoubleUniformDistribution(min, max, random);
	}

	protected final Distribution<Long> uniform(long min, long max) {
		return new LongUniformDistribution(min, max, random);
	}

	protected final Distribution<BigInteger> uniform(BigInteger min,
			BigInteger max) {
		return new BigIntegerUniformDistribution(min, max, random);
	}

	protected final Distribution<BigInteger> uniform(int numBits) {
		return new BigIntegerUniformDistribution(numBits, random);
	}

	protected final Distribution<Double> exponential(double mean) {
		return new DoubleExponentialDistribution(mean, random);
	}

	protected final Distribution<Long> exponential(long mean) {
		return new LongExponentialDistribution(mean, random);
	}

	protected final Distribution<BigInteger> exponential(BigInteger mean) {
		return new BigIntegerExponentialDistribution(mean, random);
	}

	protected final Distribution<Double> normal(double mean, double variance) {
		return new DoubleNormalDistribution(mean, variance, random);
	}

	protected final Distribution<Long> normal(long mean, long variance) {
		return new LongNormalDistribution(mean, variance, random);
	}

	protected final Distribution<BigInteger> normal(BigInteger mean,
			BigInteger variance) {
		return new BigIntegerNormalDistribution(mean, variance, random);
	}

	/**
	 * The <code>Snapshot</code> class.
	 * 
	 * @author Cosmin Arad <cosmin@sics.se>
	 * @version $Id$
	 */
	protected final static class Snapshot {
		private final TakeSnapshot takeSnapshotEvent;

		public Snapshot(TakeSnapshot takeSnapshotEvent) {
			this.takeSnapshotEvent = takeSnapshotEvent;
		}

		public void takeAfterTerminationOf(long delay,
				StochasticProcess... process) {
			HashSet<StochasticProcess> procs = new HashSet<StochasticProcess>(
					Arrays.asList(process));
			TakeSnapshotEvent snapshotEvent = new TakeSnapshotEvent(delay,
					takeSnapshotEvent, procs.size());
			for (StochasticProcess stochasticProcess : procs) {
				stochasticProcess.terminateEvent
						.setSnapshotEvent(snapshotEvent);
			}
		}
	}

	protected final Snapshot snapshot(TakeSnapshot takeSnapshotEvent) {
		return new Snapshot(takeSnapshotEvent);
	}

	/**
	 * Executes simulation.
	 * 
	 * @param main
	 */
	public final void simulate(Class<? extends ComponentDefinition> main) {
		File file = null;
		try {
			file = File.createTempFile("scenario", ".bin");
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(file));
			oos.writeObject(this);
			oos.flush();
			oos.close();
			System.setProperty("scenario", file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			Loader cl = AccessController
					.doPrivileged(new PrivilegedAction<Loader>() {
						@Override
						public Loader run() {
							return new Loader();
						}
					});
			cl.addTranslator(ClassPool.getDefault(), new TimeInterceptor(null));
			Thread.currentThread().setContextClassLoader(cl);
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			cl.run(main.getCanonicalName(), null);
		} catch (Throwable e) {
			throw new RuntimeException("Exception caught during simulation", e);
		}
	}

	public final void transform(Class<? extends ComponentDefinition> main,
			String directory) {
		Properties p = new Properties();

		File dir = null;
		File file = null;
		try {
			dir = new File(directory);
			dir.mkdirs();
			dir.setWritable(true);
			file = File.createTempFile("scenario", ".bin", dir);
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(file));
			oos.writeObject(this);
			oos.flush();
			oos.close();
			System.setProperty("scenario", file.getAbsolutePath());
			p.setProperty("scenario", file.getAbsolutePath());
			p
					.store(new FileOutputStream(file.getAbsolutePath()
							+ ".properties"), null);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			Loader cl = AccessController
					.doPrivileged(new PrivilegedAction<Loader>() {
						@Override
						public Loader run() {
							return new Loader();
						}
					});
			cl.addTranslator(ClassPool.getDefault(), new TimeInterceptor(dir));
			Thread.currentThread().setContextClassLoader(cl);
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			cl.run(main.getCanonicalName(), null);
		} catch (Throwable e) {
			throw new RuntimeException("Exception caught during simulation", e);
		}
	}

	public final void execute(Class<? extends ComponentDefinition> main) {
		File file = null;
		try {
			file = File.createTempFile("scenario", ".bin");
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(file));
			oos.writeObject(this);
			oos.flush();
			oos.close();
			System.setProperty("scenario", file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			Loader cl = AccessController
					.doPrivileged(new PrivilegedAction<Loader>() {
						@Override
						public Loader run() {
							return new Loader();
						}
					});
			Thread.currentThread().setContextClassLoader(cl);
			cl.run(main.getCanonicalName(), null);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public final LinkedList<SimulatorEvent> generateEventList() {
		LinkedList<SimulatorEvent> eventList = new LinkedList<SimulatorEvent>();
		int started = 0;
		for (StochasticProcess process : processes) {
			if (!process.relativeStartTime) {
				eventList.add(process.startEvent);
				started++;
			}
		}
		if (started == 0) {
			System.err
					.println("ERROR: Processes have circular relative start times");
		}
		if (terminatedEvent != null && !terminatedEvent.isRelativeTime()) {
			eventList.add(terminatedEvent);
		}

		return eventList;
	}

	public static SimulationScenario load(String scenarioFile) {
		SimulationScenario scenario = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
					scenarioFile));
			scenario = (SimulationScenario) ois.readObject();
			ois.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return scenario;
	}

	public final void sim(Class<? extends ComponentDefinition> main,
			String... args) {
		File file = null;
		try {
			file = File.createTempFile("scenario", ".bin");
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(file));
			oos.writeObject(this);
			oos.flush();
			oos.close();
			System.setProperty("scenario", file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		// 1. validate environment: quit if not Sun
		if (!goodEnv()) {
			throw new RuntimeException("Only Sun JRE usable for simulation");
		}

		// 2. compute boot-string
		String bootString = bootString();

		// 3. check if it already exists; goto 5 if it does
		if (!alreadyInstrumentedBoot(bootString)) {
			// 4. transform and generate boot classes in boot-string directory
			prepareInstrumentationExceptions();
			instrumentBoot(bootString);
		} else {
			prepareInstrumentationExceptions();
		}

		// 5. transform and generate application classes
		instrumentApplication();

		// 6. launch simulation process
		launchSimulation(main, args);
	}

	private void launchSimulation(Class<? extends ComponentDefinition> main,
			String... args) {
		LinkedList<String> arguments = new LinkedList<String>();

		String java = System.getProperty("java.home");
		String sep = System.getProperty("file.separator");
		String pathSep = System.getProperty("path.separator");
		java += sep + "bin" + sep + "java";

		if (System.getProperty("os.name").startsWith("Windows")) {
			arguments.add("\"" + java + "\"");
		} else {
			arguments.add(java);
		}

		arguments.add("-Xbootclasspath:" + directory + bootString() + pathSep
				+ directory + "application");

		arguments.add("-classpath");
		arguments.add(directory + "application");

		arguments.addAll(getJvmArgs(args));

		arguments.add("-Dscenario=" + System.getProperty("scenario"));

		arguments.add(main.getName());

		arguments.addAll(getApplicationArgs(args));

		ProcessBuilder pb = new ProcessBuilder(arguments);
		pb.redirectErrorStream(true);

		saveSimulationCommandLine(arguments);

		try {
			Process process = pb.start();
			BufferedReader out = new BufferedReader(new InputStreamReader(
					process.getInputStream()));

			String line;
			do {
				line = out.readLine();
				if (line != null) {
					System.out.println(line);
				}
			} while (line != null);
		} catch (IOException e) {
			throw new RuntimeException("Cannot start simulation process", e);
		}
	}

	private void saveSimulationCommandLine(final LinkedList<String> args) {
		File file = null;
		try {
			// Windows batch file
			file = new File(directory + "run-simulation.bat");
			PrintStream ps = new PrintStream(file);

			for (String arg : args) {
				ps.println(arg.replaceAll(directory, "") + "\t^");
				// ps.println(arg + "\t^");
			}
			ps.println(";");

			ps.flush();
			ps.close();

			// Linux/Unix Bash script
			file = new File(directory + "run-simulation.sh");
			ps = new PrintStream(file);
			ps.println("#!/bin/bash");
			ps.println();

			for (String arg : args) {
				ps.println(arg.replaceAll(directory, "") + "\t\\");
				// ps.println(arg + "\t\\");
			}
			ps.println(";");

			ps.flush();
			ps.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private LinkedList<String> getJvmArgs(String[] args) {
		LinkedList<String> list = new LinkedList<String>();
		boolean maxHeap = false;

		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("-JVM:")) {
				String a = args[i].substring(5);

				if (a.startsWith("-Xmx")) {
					maxHeap = true;
				}
				if (a.startsWith("-Xbootclasspath") || a.startsWith("-cp")
						|| a.startsWith("-classpath")) {
					continue; // ignore class-path settings
				}
				list.add(a);
			}
		}

		if (!maxHeap) {
			list.add("-Xmx1g");
		}
		return list;
	}

	private LinkedList<String> getApplicationArgs(String[] args) {
		LinkedList<String> list = new LinkedList<String>();
		for (int i = 0; i < args.length; i++) {
			if (!args[i].startsWith("-JVM:")) {
				list.add(args[i]);
			}
		}
		return list;
	}

	private static final String directory = "./target/kompics-simulation/";
	private static HashSet<String> exceptions = new HashSet<String>();

	private void prepareInstrumentationExceptions() {
		// well known exceptions
		exceptions.add("java.lang.ref.Reference");
		exceptions.add("java.lang.ref.Finalizer");
		exceptions.add("se.sics.kompics.p2p.simulator.P2pSimulator");
		exceptions.add("org.apache.log4j.PropertyConfigurator");
		exceptions.add("org.apache.log4j.helpers.FileWatchdog");
		exceptions.add("org.mortbay.thread.QueuedThreadPool");
		exceptions.add("org.mortbay.io.nio.SelectorManager");
		exceptions.add("org.mortbay.io.nio.SelectorManager$SelectSet");
		exceptions
				.add("org.apache.commons.math.stat.descriptive.SummaryStatistics");
		exceptions
				.add("org.apache.commons.math.stat.descriptive.DescriptiveStatistics");

		// try to add user-defined exceptions from properties file
		InputStream in = ClassLoader
				.getSystemResourceAsStream("timer.interceptor.properties");
		Properties p = new Properties();
		if (in != null) {
			try {
				p.load(in);
				for (String classname : p.stringPropertyNames()) {
					String value = p.getProperty(classname);
					if (value != null && value.equals("IGNORE")) {
						exceptions.add(classname);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void instrumentBoot(String bootString) {
		String bootCp = System.getProperty("sun.boot.class.path");
		try {
			transformClasses(bootCp, bootString);
			copyResources(bootCp, bootString);
		} catch (Throwable t) {
			throw new RuntimeException(
					"Exception caught while preparing simulation", t);
		}
	}

	private void instrumentApplication() {
		String cp = System.getProperty("java.class.path");
		try {
			transformClasses(cp, null);
			copyResources(cp, null);
		} catch (Throwable t) {
			throw new RuntimeException(
					"Exception caught while preparing simulation", t);
		}
	}

	private void transformClasses(String classPath, String boot)
			throws IOException, NotFoundException, CannotCompileException {
		LinkedList<String> classes = getAllClasses(classPath);

		ClassPool pool = new ClassPool();
		pool.appendSystemPath();

		String target = directory;
		if (boot == null) {
			pool.appendPathList(classPath);
			target += "application";
			logger.info("Instrumenting application classes to:" + target);
		} else {
			target += boot;
			logger.info("Instrumenting bootstrap classes to:" + target);
		}
		CodeInstrumenter ci = new CodeInstrumenter();

		int count = classes.size();
		long start = System.currentTimeMillis();

		for (final String classname : classes) {

			int d = classname.indexOf("$");
			String outerClass = (d == -1 ? classname : classname
					.substring(0, d));

			CtClass ctc = pool.getCtClass(classname);

			if (!exceptions.contains(outerClass)) {
				ctc.instrument(ci);
			} else {
				logger.trace("Skipping " + classname);
			}
			saveClass(ctc, target);
		}

		long stop = System.currentTimeMillis();
		logger.info("It took " + (stop - start) + "ms to instrument " + count
				+ " classes.");
	}

	private boolean alreadyInstrumentedBoot(String bootString) {
		File f = new File(directory + bootString);
		return f.exists() && f.isDirectory();
	}

	private String bootString() {
		String os = System.getProperty("os.name");
		int sp = os.indexOf(' ');
		if (sp != -1) {
			os = os.substring(0, sp);
		}
		String vendor = System.getProperty("java.vendor");
		sp = vendor.indexOf(' ');
		if (sp != -1) {
			vendor = vendor.substring(0, sp);
		}

		return "boot-" + vendor + "-" + System.getProperty("java.version")
				+ "-" + os + "-" + System.getProperty("os.arch");
	}

	private boolean goodEnv() {
		if (System.getProperty("java.vendor").startsWith("Sun"))
			return true;
		// we should change this method to accept more (or less) Java
		// environments known to be (un)acceptable for our instrumentation
		return false;
	}

	private void saveClass(CtClass cc, String dir) {
		File directory = new File(dir);
		if (directory != null) {
			try {
				cc.writeFile(directory.getAbsolutePath());
			} catch (CannotCompileException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void copyResources(String classPath, String boot)
			throws IOException {
		LinkedList<String> resources = getAllResources(classPath);

		String target = directory;
		if (boot == null) {
			target += "application";
			logger.info("Copying application resources to:" + target);
		} else {
			target += boot;
			logger.info("Copying bootstrap resources to:" + target);
		}

		int count = resources.size();
		long start = System.currentTimeMillis();

		for (final String resourceName : resources) {
			InputStream is = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream(resourceName);

			String targetFile = target + "/" + resourceName;
			File dir = new File(new File(targetFile).getParent());
			if (!dir.exists()) {
				dir.mkdirs();
				dir.setWritable(true);
			}
			OutputStream os = new FileOutputStream(targetFile);
			byte buffer[] = new byte[65536];
			int len;

			long ms = System.currentTimeMillis();

			// copy the resource
			while ((len = is.read(buffer)) > 0) {
				os.write(buffer, 0, len);
			}
			is.close();
			os.close();

			ms = System.currentTimeMillis() - ms;
			logger.trace("Copying " + resourceName + " to "
					+ (target + "/" + resourceName) + " - took " + ms + "ms.");
		}

		long stop = System.currentTimeMillis();
		logger.info("It took " + (stop - start) + "ms to copy " + count
				+ " resources.");
	}

	private LinkedList<String> getAllClasses(String cp) throws IOException {
		LinkedList<String> list = new LinkedList<String>();

		for (String location : getAllLocations(cp)) {
			list.addAll(getClassesFromLocation(location));
		}
		return list;
	}

	private LinkedList<String> getAllResources(String cp) throws IOException {
		LinkedList<String> list = new LinkedList<String>();

		for (String location : getAllLocations(cp)) {
			list.addAll(getResourcesFromLocation(location));
		}
		return list;
	}

	private LinkedList<String> getAllLocations(String cp) {
		LinkedList<String> list = new LinkedList<String>();

		for (String string : cp.split(System.getProperty("path.separator"))) {
			list.add(string);
		}
		return list;
	}

	private LinkedList<String> getClassesFromLocation(String location)
			throws IOException {
		File f = new File(location);

		if (f.exists() && f.isDirectory()) {
			return getClassesFromDirectory(f, "");
		}
		if (f.exists() && f.isFile() && f.getName().endsWith(".jar")) {
			return getClassesFromJar(f);
		}

		LinkedList<String> list = new LinkedList<String>();
		return list;
	}

	private LinkedList<String> getClassesFromJar(File jar) throws IOException {
		JarFile j = new JarFile(jar);

		LinkedList<String> list = new LinkedList<String>();

		// System.err.println("Jar entries: " + j.size());

		Enumeration<JarEntry> entries = j.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();

			if (entry.getName().endsWith(".class")) {
				String className = entry.getName().substring(0,
						entry.getName().lastIndexOf('.'));
				list.add(className.replace('/', '.'));
			}
			// System.err.println(entry);
		}
		return list;
	}

	private LinkedList<String> getClassesFromDirectory(File directory,
			String pack) {
		String[] files = directory.list();

		LinkedList<String> list = new LinkedList<String>();
		for (String string : files) {
			File f = new File(directory + System.getProperty("file.separator")
					+ string);

			if (f.isFile() && f.getName().endsWith(".class")) {
				String className = f.getName().substring(0,
						f.getName().lastIndexOf('.'));
				list.add(pack + className);
			}

			if (f.isDirectory()) {
				LinkedList<String> classes = getClassesFromDirectory(f, pack
						+ f.getName() + ".");
				list.addAll(classes);
			}
		}
		return list;
	}

	private LinkedList<String> getResourcesFromLocation(String location)
			throws IOException {
		File f = new File(location);

		if (f.exists() && f.isDirectory()) {
			return getResourcesFromDirectory(f, "");
		}
		if (f.exists() && f.isFile() && f.getName().endsWith(".jar")) {
			return getResourcesFromJar(f);
		}

		LinkedList<String> list = new LinkedList<String>();
		return list;
	}

	private LinkedList<String> getResourcesFromJar(File jar) throws IOException {
		JarFile j = new JarFile(jar);

		LinkedList<String> list = new LinkedList<String>();

		Enumeration<JarEntry> entries = j.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();

			if (!entry.getName().endsWith(".class") && !entry.isDirectory()
					&& !entry.getName().startsWith("META-INF")) {
				String resourceName = entry.getName();
				list.add(resourceName);
			}
		}
		return list;
	}

	private LinkedList<String> getResourcesFromDirectory(File directory,
			String pack) {
		String[] files = directory.list();

		LinkedList<String> list = new LinkedList<String>();
		for (String string : files) {
			File f = new File(directory + System.getProperty("file.separator")
					+ string);

			if (f.isFile() && !f.getName().endsWith(".class")) {
				String resourceName = f.getName();
				list.add(pack + resourceName);
			}

			if (f.isDirectory()) {
				LinkedList<String> resources = getResourcesFromDirectory(f,
						pack + f.getName() + "/");
				list.addAll(resources);
			}
		}
		return list;
	}
}
