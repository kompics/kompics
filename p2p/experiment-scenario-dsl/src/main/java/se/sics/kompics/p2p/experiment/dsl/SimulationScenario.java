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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Random;
import java.util.TimeZone;

import javassist.ClassPool;
import javassist.Loader;
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
}
