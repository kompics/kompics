package se.sics.kompics.kdld.slave;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Event;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.PortType;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.simulator.NetworkModel;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.events.KompicsSimulatorEvent;
import se.sics.kompics.simulator.events.SimulationTerminatedEvent;
import se.sics.kompics.simulator.events.SimulatorEvent;
import se.sics.kompics.simulator.events.StochasticProcessEvent;
import se.sics.kompics.simulator.events.StochasticProcessStartEvent;
import se.sics.kompics.simulator.events.StochasticProcessTerminatedEvent;
import se.sics.kompics.simulator.events.TakeSnapshotEvent;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;

/**
 * XXX Bootstrap Client functionality for session mgmt.
 * XXX 
 * 
 * @author jdowling
 *
 */
public final class Slave extends ComponentDefinition {
	private static Class<? extends PortType> simulationPortType;

	public static void setSimulationPortType(Class<? extends PortType> portType) {
		simulationPortType = portType;
	}

	private static final Logger logger = LoggerFactory
			.getLogger(Slave.class);

	Negative<?> simulationPort = negative(simulationPortType);
	Negative<Network> network = negative(Network.class);
	Negative<Timer> timer = negative(Timer.class);

	private SimulationScenario scenario;
	private final Slave master;

	private Random random;

	private NetworkModel networkModel;

	// set of active timers
	private final HashMap<UUID, TimerSignalTask> activeTimers;
	// set of active periodic timers
	private final HashMap<UUID, PeriodicTimerSignalTask> activePeriodicTimers;
	private final java.util.Timer javaTimer;

	public Slave() {
		activeTimers = new HashMap<UUID, TimerSignalTask>();
		activePeriodicTimers = new HashMap<UUID, PeriodicTimerSignalTask>();
		javaTimer = new java.util.Timer("JavaTimer@"
				+ Integer.toHexString(this.hashCode()));
		master = this;

		subscribe(handleInit, control);
		subscribe(handleMessage, network);
		subscribe(handleST, timer);
		subscribe(handleSPT, timer);
		subscribe(handleCT, timer);
		subscribe(handleCPT, timer);
	}

	private String pName(SimulatorEvent event) {
		if (event instanceof StochasticProcessEvent) {
			return ((StochasticProcessEvent) event).getProcessName();
		} else if (event instanceof StochasticProcessStartEvent) {
			return ((StochasticProcessStartEvent) event).getProcessName();
		} else if (event instanceof StochasticProcessTerminatedEvent) {
			return ((StochasticProcessTerminatedEvent) event).getProcessName();
		}
		return "";
	}

	private boolean executeEvent(SimulatorEvent event) {
		if (event instanceof StochasticProcessEvent) {
			executeStochasticProcessEvent((StochasticProcessEvent) event);
		} else if (event instanceof StochasticProcessStartEvent) {
			executeStochasticProcessStartEvent((StochasticProcessStartEvent) event);
		} else if (event instanceof StochasticProcessTerminatedEvent) {
			executeStochasticProcessTerminatedEvent((StochasticProcessTerminatedEvent) event);
		} else if (event instanceof KompicsSimulatorEvent) {
			executeKompicsEvent(((KompicsSimulatorEvent) event).getEvent());
		} else if (event instanceof TakeSnapshotEvent) {
			executeTakeSnapshotEvent((TakeSnapshotEvent) event);
		} else if (event instanceof SimulationTerminatedEvent) {
			return executeSimultationTerminationEvent((SimulationTerminatedEvent) event);
		}
		return true;
	}

	private void executeStochasticProcessStartEvent(
			StochasticProcessStartEvent event) {
		if (event.shouldHandleNow()) {
			logger.debug("Started " + pName(event));
			// trigger start events relative to this one
			LinkedList<StochasticProcessStartEvent> startEvents = event
					.getStartEvents();
			for (StochasticProcessStartEvent startEvent : startEvents) {
				long delay = startEvent.getDelay();
				if (delay > 0) {
					javaTimer.schedule(new SimulatorEventTask(master,
							startEvent), delay);
				} else {
					handleSimulatorEvent(startEvent);
				}
			}
			// get the stochastic process running
			StochasticProcessEvent stochasticEvent = event.getStochasticEvent();
			handleSimulatorEvent(stochasticEvent);
		}
	}

	private void executeStochasticProcessTerminatedEvent(
			StochasticProcessTerminatedEvent event) {
		logger.debug("Terminated process " + pName(event));
		// trigger start events relative to this process termination
		LinkedList<StochasticProcessStartEvent> startEvents = event
				.getStartEvents();
		for (StochasticProcessStartEvent startEvent : startEvents) {
			long delay = startEvent.getDelay();
			if (delay > 0) {
				javaTimer.schedule(new SimulatorEventTask(master,
						startEvent), delay);
			} else {
				handleSimulatorEvent(startEvent);
			}
		}
		// trigger simulation termination relative to this process termination
		TakeSnapshotEvent snapshotEvent = event.getSnapshotEvent();
		if (snapshotEvent != null) {
			long delay = snapshotEvent.getDelay();
			if (delay > 0) {
				javaTimer.schedule(new SimulatorEventTask(master,
						snapshotEvent), delay);
			} else {
				handleSimulatorEvent(snapshotEvent);
			}
		}
		SimulationTerminatedEvent terminatedEvent = event.getTerminationEvent();
		if (terminatedEvent != null) {
			long delay = terminatedEvent.getDelay();
			if (delay > 0) {
				javaTimer.schedule(new SimulatorEventTask(master,
						terminatedEvent), delay);
			} else {
				handleSimulatorEvent(terminatedEvent);
			}
		}
	}

	private void executeStochasticProcessEvent(StochasticProcessEvent event) {
		Event e = event.generateOperation(random);

		trigger(e, simulationPort);
		logger.debug("{}: {}", pName(event), e);

		if (event.getCurrentCount() > 0) {
			// still have operations to generate, reschedule
			long delay = event.getNextTime();
			if (delay > 0) {
				javaTimer.schedule(new SimulatorEventTask(master,
						event), delay);
			} else {
				handleSimulatorEvent(event);
			}
		} else {
			// no operations left. stochastic process terminated
			handleSimulatorEvent(event.getTerminatedEvent());
		}
	}

	private void executeKompicsEvent(Event kompicsEvent) {
		// trigger other Kompics events on the simulation port
		logger.debug("KOMPICS_EVENT {}", kompicsEvent.getClass());
		trigger(kompicsEvent, simulationPort);
	}

	private void executeTakeSnapshotEvent(TakeSnapshotEvent event) {
		if (event.shouldHandleNow()) {
			trigger(event.getTakeSnapshotEvent(), simulationPort);
		}
	}

	private boolean executeSimultationTerminationEvent(
			SimulationTerminatedEvent event) {
		if (event.shouldTerminateNow()) {
			logger.info("Orchestration terminated.");
			return false;
		}
		return true;
	}

	Handler<SlaveInit> handleInit = new Handler<SlaveInit>() {
		public void handle(SlaveInit init) {
			scenario = init.getScenario();
			random = scenario.getRandom();

			// generate initial future events from the scenario
			LinkedList<SimulatorEvent> events = scenario.generateEventList();
			for (SimulatorEvent simulatorEvent : events) {
				long time = simulatorEvent.getTime();
				if (time == 0) {
					handleSimulatorEvent(simulatorEvent);
				} else {
					SimulatorEventTask task = new SimulatorEventTask(
							master, simulatorEvent);
					javaTimer.schedule(task, simulatorEvent.getTime());
				}
			}
			networkModel = init.getNetworkModel();

			logger.info("Orchestration started");
		}
	};

	Handler<Message> handleMessage = new Handler<Message>() {
		public void handle(Message event) {
			random.nextInt();
			logger.debug("Message send: {}", event);

			if (networkModel != null) {
				long latency = networkModel.getLatencyMs(event);

				if (latency > 0) {
					DelayedMessageTask task = new DelayedMessageTask(
							master, event);
					javaTimer.schedule(task, latency);
					return;
				}
			}
			// we just echo the message on the network port
			trigger(event, network);
		}
	};

	Handler<ScheduleTimeout> handleST = new Handler<ScheduleTimeout>() {
		public void handle(ScheduleTimeout event) {
			logger.debug("ScheduleTimeout@{} : {}", event.getDelay(), event
					.getTimeoutEvent());

			if (event.getDelay() < 0) {
				throw new RuntimeException(
						"Cannot set a negative timeout value.");
			}

			UUID id = event.getTimeoutEvent().getTimeoutId();
			TimerSignalTask timeOutTask = new TimerSignalTask(master,
					event.getTimeoutEvent(), id);

			synchronized (activeTimers) {
				activeTimers.put(id, timeOutTask);
			}
			javaTimer.schedule(timeOutTask, event.getDelay());
		}
	};

	Handler<SchedulePeriodicTimeout> handleSPT = new Handler<SchedulePeriodicTimeout>() {
		public void handle(SchedulePeriodicTimeout event) {
			logger.debug("SchedulePeriodicTimeout@{} : {}", event.getPeriod(),
					event.getTimeoutEvent());

			if (event.getDelay() < 0 || event.getPeriod() < 0)
				throw new RuntimeException(
						"Cannot set a negative timeout value.");

			UUID id = event.getTimeoutEvent().getTimeoutId();
			PeriodicTimerSignalTask timeOutTask = new PeriodicTimerSignalTask(
					event.getTimeoutEvent(), master);

			synchronized (activePeriodicTimers) {
				activePeriodicTimers.put(id, timeOutTask);
			}
			javaTimer.scheduleAtFixedRate(timeOutTask, event.getDelay(), event
					.getPeriod());
		}
	};

	Handler<CancelTimeout> handleCT = new Handler<CancelTimeout>() {
		public void handle(CancelTimeout event) {
			UUID id = event.getTimeoutId();
			logger.debug("CancelTimeout: {}", id);

			TimerSignalTask task = null;
			synchronized (activeTimers) {
				task = activeTimers.get(id);
				if (task != null) {
					task.cancel();
					activeTimers.remove(id);
					logger.debug("canceled timer {}", task.timeout);
				} else {
					logger.warn("Cannot find timeout {} from {}", id, event.getStackTrace()[2]);
				}
			}
		}
	};

	Handler<CancelPeriodicTimeout> handleCPT = new Handler<CancelPeriodicTimeout>() {
		public void handle(CancelPeriodicTimeout event) {
			UUID id = event.getTimeoutId();
			logger.debug("CancelPeridicTimeout: {}", id);

			PeriodicTimerSignalTask task = null;
			synchronized (activePeriodicTimers) {
				task = activePeriodicTimers.get(id);
				if (task != null) {
					task.cancel();
					activePeriodicTimers.remove(id);
					logger.debug("canceled periodic timer {}", task.timeout);
				} else {
					logger.warn("Cannot find periodic timeout {}", id);
				}
			}
		}
	};

	final void timeout(UUID timerId, Timeout timeout) {
		synchronized (activeTimers) {
			activeTimers.remove(timerId);
		}
		logger.debug("trigger timeout {}", timeout);
		trigger(timeout, timer);
	}

	final void periodicTimeout(Timeout timeout) {
		logger.debug("trigger periodic timeout {}", timeout);
		trigger(timeout, timer);
	}

	final void deliverDelayedMessage(Message message) {
		logger.debug("trigger message {}", message);
		trigger(message, network);
	}

	final synchronized void handleSimulatorEvent(SimulatorEvent event) {
		logger.debug("trigger event {}", event);
		executeEvent(event);
	}
}
