/**
 * This file is part of the Kompics P2P Framework.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.kompics.p2p.orchestrator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Negative;
import se.sics.kompics.PortType;
import se.sics.kompics.Start;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.model.common.NetworkModel;
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;
import se.sics.kompics.p2p.experiment.dsl.events.KompicsSimulatorEvent;
import se.sics.kompics.p2p.experiment.dsl.events.SimulationTerminatedEvent;
import se.sics.kompics.p2p.experiment.dsl.events.SimulatorEvent;
import se.sics.kompics.p2p.experiment.dsl.events.StochasticProcessEvent;
import se.sics.kompics.p2p.experiment.dsl.events.StochasticProcessStartEvent;
import se.sics.kompics.p2p.experiment.dsl.events.StochasticProcessTerminatedEvent;
import se.sics.kompics.p2p.experiment.dsl.events.TakeSnapshotEvent;
import se.sics.kompics.p2p.experiment.dsl.events.TerminateExperiment;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;

/**
 * The
 * <code>P2pOrchestrator</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class P2pOrchestrator extends ComponentDefinition {

    private static Class<? extends PortType> simulationPortType;

    public static void setSimulationPortType(Class<? extends PortType> portType) {
        simulationPortType = portType;
    }
    static final Logger logger = LoggerFactory
            .getLogger(P2pOrchestrator.class);
    Negative<?> simulationPort = negative(simulationPortType);
    Negative<Network> network = negative(Network.class);
    Negative<Timer> timer = negative(Timer.class);
    private SimulationScenario scenario;
    private final P2pOrchestrator thisOrchestrator;
    private Random random;
    private NetworkModel networkModel;
    // set of active timers
    private final HashMap<UUID, TimerSignalTask> activeTimers;
    // set of active periodic timers
    private final HashMap<UUID, PeriodicTimerSignalTask> activePeriodicTimers;
    private final java.util.Timer javaTimer;

    public P2pOrchestrator(P2pOrchestratorInit init) {
        activeTimers = new HashMap<UUID, TimerSignalTask>();
        activePeriodicTimers = new HashMap<UUID, PeriodicTimerSignalTask>();
        javaTimer = new java.util.Timer("JavaTimer@"
                + Integer.toHexString(this.hashCode()));
        thisOrchestrator = this;

        subscribe(handleStart, control);
        subscribe(handleMessage, network);
        subscribe(handleST, timer);
        subscribe(handleSPT, timer);
        subscribe(handleCT, timer);
        subscribe(handleCPT, timer);

        // INIT
        scenario = init.getScenario();
        random = scenario.getRandom();
        networkModel = init.getNetworkModel();
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
                    javaTimer.schedule(new SimulatorEventTask(thisOrchestrator,
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
                javaTimer.schedule(new SimulatorEventTask(thisOrchestrator,
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
                javaTimer.schedule(new SimulatorEventTask(thisOrchestrator,
                        snapshotEvent), delay);
            } else {
                handleSimulatorEvent(snapshotEvent);
            }
        }
        SimulationTerminatedEvent terminatedEvent = event.getTerminationEvent();
        if (terminatedEvent != null) {
            long delay = terminatedEvent.getDelay();
            if (delay > 0) {
                javaTimer.schedule(new SimulatorEventTask(thisOrchestrator,
                        terminatedEvent), delay);
            } else {
                handleSimulatorEvent(terminatedEvent);
            }
        }
    }

    private void executeStochasticProcessEvent(StochasticProcessEvent event) {
        KompicsEvent e = event.generateOperation(random);

        trigger(e, simulationPort);
        logger.debug("{}: {}", pName(event), e);

        if (event.getCurrentCount() > 0) {
            // still have operations to generate, reschedule
            long delay = event.getNextTime();
            if (delay > 0) {
                javaTimer.schedule(new SimulatorEventTask(thisOrchestrator,
                        event), delay);
            } else {
                handleSimulatorEvent(event);
            }
        } else {
            // no operations left. stochastic process terminated
            handleSimulatorEvent(event.getTerminatedEvent());
        }
    }

    private void executeKompicsEvent(KompicsEvent kompicsEvent) {
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
            try {
                trigger(new TerminateExperiment(), simulationPort);
            } catch (Exception e) {
                logger.warn("Could not trigger TerminateExperiment on the SimulationPort");
            }
            logger.info("Orchestration terminated.");
            return false;
        }
        return true;
    }
    
    /**
     * This init handler should be called after all other init handlers have been
     * called, as it may immediately start generating events that would otherwise
     * just get dropped.
     */
    Handler<Start> handleStart = new Handler<Start>() {
        public void handle(Start init) {
            logger.info("Orchestration started");

            // generate initial future events from the scenario
            LinkedList<SimulatorEvent> events = scenario.generateEventList();
            for (SimulatorEvent simulatorEvent : events) {
                long time = simulatorEvent.getTime();
                if (time == 0) {
                    handleSimulatorEvent(simulatorEvent);
                } else {
                    SimulatorEventTask task = new SimulatorEventTask(
                            thisOrchestrator, simulatorEvent);
                    javaTimer.schedule(task, time);
                }
            }
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
                            thisOrchestrator, event);
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
            TimerSignalTask timeOutTask = new TimerSignalTask(thisOrchestrator,
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

            if (event.getDelay() < 0 || event.getPeriod() < 0) {
                throw new RuntimeException(
                        "Cannot set a negative timeout value.");
            }

            UUID id = event.getTimeoutEvent().getTimeoutId();
            PeriodicTimerSignalTask timeOutTask = new PeriodicTimerSignalTask(
                    event.getTimeoutEvent(), thisOrchestrator);

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
                    logger.debug("Cannot find timeout {}", id);
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
                    logger.debug("Cannot find periodic timeout {}", id);
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
