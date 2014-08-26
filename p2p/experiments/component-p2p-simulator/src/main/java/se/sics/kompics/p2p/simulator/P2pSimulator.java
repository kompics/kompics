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
package se.sics.kompics.p2p.simulator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
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
import se.sics.kompics.p2p.experiment.dsl.events.PeriodicSimulatorEvent;
import se.sics.kompics.p2p.experiment.dsl.events.SimulationTerminatedEvent;
import se.sics.kompics.p2p.experiment.dsl.events.SimulatorEvent;
import se.sics.kompics.p2p.experiment.dsl.events.StochasticProcessEvent;
import se.sics.kompics.p2p.experiment.dsl.events.StochasticProcessStartEvent;
import se.sics.kompics.p2p.experiment.dsl.events.StochasticProcessTerminatedEvent;
import se.sics.kompics.p2p.experiment.dsl.events.TakeSnapshotEvent;
import se.sics.kompics.p2p.experiment.dsl.events.TerminateExperiment;
import se.sics.kompics.simulation.Simulator;
import se.sics.kompics.simulation.SimulatorScheduler;
import se.sics.kompics.simulation.SimulatorSystem;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;

/**
 * The
 * <code>P2pSimulator</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class P2pSimulator extends ComponentDefinition implements
        Simulator {

    private static Class<? extends PortType> simulationPortType;

    public static void setSimulationPortType(Class<? extends PortType> portType) {
        simulationPortType = portType;
    }
    private static final Logger logger = LoggerFactory
            .getLogger(P2pSimulator.class);
    Negative<?> simulationPort = negative(simulationPortType);
    Negative<Network> network = negative(Network.class);
    Negative<Timer> timer = negative(Timer.class);
    private SimulatorScheduler scheduler;
    private SimulationScenario scenario;
    private Simulator thisSimulator;
    private long CLOCK;
    private Random random;
    private FutureEventList futureEventList;
    private NetworkModel networkModel;
    // set of active timers
    private final HashMap<UUID, KompicsSimulatorEvent> activeTimers;
    // set of active periodic timers
    private final HashMap<UUID, PeriodicSimulatorEvent> activePeriodicTimers;
    // time statistics
    private long simulationStartTime = 0;

    public P2pSimulator(P2pSimulatorInit init) {
        // set myself as the simulated time provider
        SimulatorSystem.setSimulator(this);

        thisSimulator = this;
        futureEventList = new FutureEventList();
        activeTimers = new HashMap<UUID, KompicsSimulatorEvent>();
        activePeriodicTimers = new HashMap<UUID, PeriodicSimulatorEvent>();

        subscribe(handleMessage, network);
        subscribe(handleST, timer);
        subscribe(handleSPT, timer);
        subscribe(handleCT, timer);
        subscribe(handleCPT, timer);
        subscribe(handleTerminate, simulationPort);

        //INIT
        scheduler = init.getScheduler();
        scheduler.setSimulator(thisSimulator);
        scenario = init.getScenario();
        random = scenario.getRandom();

        CLOCK = 0;
        simulationStartTime = System.currentTimeMillis();


        networkModel = init.getNetworkModel();


    }

    public boolean advanceSimulation() {
        SimulatorEvent event = futureEventList.getAndRemoveFirstEvent(CLOCK);
        if (event == null) {
            logger.error("Simulator ran out of events.");
            logTimeStatistics();
            return false;
        }

        long time = event.getTime();

        if (time < CLOCK) {
            throw new RuntimeException("Future event has past timestamp."
                    + " CLOCK=" + CLOCK + " event=" + time + event);
        }
        CLOCK = time;

        // execute this event
        boolean ok = executeEvent(event);
        if (!ok) {
            return false;
        }
        // execute all events scheduled to occur at the same time
        while (futureEventList.hasMoreEventsAtTime(CLOCK)) {
            event = futureEventList.getAndRemoveFirstEvent(CLOCK);
            ok = executeEvent(event);
            if (!ok) {
                return false;
            }
        }
        return true;
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
        } else if (event instanceof PeriodicSimulatorEvent) {
            executePeriodicSimulatorEvent((PeriodicSimulatorEvent) event);
        } else if (event instanceof KompicsSimulatorEvent) {
            KompicsSimulatorEvent kse = (KompicsSimulatorEvent) event;
            if (!kse.canceled()) {
                executeKompicsEvent(kse.getEvent());
            }
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
                startEvent.setTime(CLOCK);
                futureEventList.scheduleFutureEvent(CLOCK, startEvent);
            }
            // get the stochastic process running
            StochasticProcessEvent stochasticEvent = event.getStochasticEvent();
            stochasticEvent.setTime(CLOCK);
            futureEventList.scheduleFutureEvent(CLOCK, stochasticEvent);
        }
    }

    private void executeStochasticProcessTerminatedEvent(
            StochasticProcessTerminatedEvent event) {
        logger.debug("Terminated process " + pName(event));
        // trigger start events relative to this process termination
        LinkedList<StochasticProcessStartEvent> startEvents = event
                .getStartEvents();
        for (StochasticProcessStartEvent startEvent : startEvents) {
            startEvent.setTime(CLOCK);
            futureEventList.scheduleFutureEvent(CLOCK, startEvent);
        }

        // trigger snapshot relative to this process termination
        TakeSnapshotEvent snapshotEvent = event.getSnapshotEvent();
        if (snapshotEvent != null) {
            if (snapshotEvent.isOnList()) {
                boolean removed = futureEventList.cancelFutureEvent(CLOCK,
                        snapshotEvent);
                if (!removed) {
                    throw new RuntimeException(
                            "Event should have been scheduled:" + snapshotEvent);
                }
                snapshotEvent.shouldHandleNow();
            }
            snapshotEvent.setTime(CLOCK);
            futureEventList.scheduleFutureEvent(CLOCK, snapshotEvent);
        }

        // trigger simulation termination relative to this process termination
        SimulationTerminatedEvent terminationEvent = event
                .getTerminationEvent();
        if (terminationEvent != null) {
            if (terminationEvent.isOnList()) {
                boolean removed = futureEventList.cancelFutureEvent(CLOCK,
                        terminationEvent);
                if (!removed) {
                    throw new RuntimeException(
                            "Event should have been scheduled:"
                            + terminationEvent);
                }
                terminationEvent.shouldTerminateNow();
            }
            terminationEvent.setTime(CLOCK);
            futureEventList.scheduleFutureEvent(CLOCK, terminationEvent);
        }
    }

    private void executeStochasticProcessEvent(StochasticProcessEvent event) {
        KompicsEvent e = event.generateOperation(random);

        trigger(e, simulationPort);
        logger.debug("{}: {}", pName(event), e);

        if (event.getCurrentCount() > 0) {
            // still have operations to generate, reschedule
            event.setNextTime();
            futureEventList.scheduleFutureEvent(CLOCK, event);
        } else {
            // no operations left. stochastic process terminated
            StochasticProcessTerminatedEvent t = event.getTerminatedEvent();
            t.setTime(CLOCK);
            futureEventList.scheduleFutureEvent(CLOCK, t);
        }
    }

    private void executeKompicsEvent(KompicsEvent kompicsEvent) {
        // trigger Messages on the Network port
        if (Message.class.isAssignableFrom(kompicsEvent.getClass())) {
            Message message = (Message) kompicsEvent;
            logger.debug("Delivered Message: {} from {} to {} ", new Object[]{
                        message, message.getSource(), message.getDestination()});
            trigger(kompicsEvent, network);
            return;
        }

        // trigger Timeouts on the Timer port
        if (Timeout.class.isAssignableFrom(kompicsEvent.getClass())) {
            Timeout timeout = (Timeout) kompicsEvent;
            logger.debug("Triggered Timeout: {} {}", kompicsEvent, timeout
                    .getTimeoutId());
            activeTimers.remove(timeout.getTimeoutId());
            trigger(kompicsEvent, timer);
            return;
        }

        // trigger other Kompics events on the simulation port
        trigger(kompicsEvent, simulationPort);
    }

    private void executePeriodicSimulatorEvent(PeriodicSimulatorEvent periodic) {
        // reschedule periodic event
        periodic.setTime(CLOCK + periodic.getPeriod());

        // clone timeouts
        if (Timeout.class.isAssignableFrom(periodic.getEvent().getClass())) {
            Timeout timeout = (Timeout) periodic.getEvent();
            try {
                periodic.setEvent((Timeout) timeout.clone());
            } catch (CloneNotSupportedException ex) {
                logger.warn("Could not clone Timeout event", ex);
            }

            logger.debug("Triggered [periodic] Timeout: {} {}", timeout,
                    timeout.getTimeoutId());
            trigger(timeout, timer);
        }
        futureEventList.scheduleFutureEvent(CLOCK, periodic);
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

            logger.info("Simulation terminated.");
            logTimeStatistics();
            return false;
        }
        return true;
    }
    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            // generate initial future events from the scenario
            LinkedList<SimulatorEvent> events = scenario.generateEventList();
            for (SimulatorEvent simulatorEvent : events) {
                futureEventList.scheduleFutureEvent(CLOCK, simulatorEvent);
            }
            logger.info("Simulation started");
        }
    };
    Handler<Message> handleMessage = new Handler<Message>() {
        public void handle(Message event) {
            random.nextInt();
            logger.debug("Message send: {}", event);

            if (networkModel != null) {
                long latency = networkModel.getLatencyMs(event);
                futureEventList.scheduleFutureEvent(CLOCK,
                        new KompicsSimulatorEvent(event, CLOCK + latency));
            } else {
                // we just echo the message on the network port
                trigger(event, network);
            }
        }
    };
    Handler<ScheduleTimeout> handleST = new Handler<ScheduleTimeout>() {
        public void handle(ScheduleTimeout event) {
            logger.debug("ScheduleTimeout@{} : {} {} AT={}", new Object[]{
                        event.getDelay(), event.getTimeoutEvent(),
                        event.getTimeoutEvent().getTimeoutId(),
                        activeTimers.keySet()});

            if (event.getDelay() < 0) {
                throw new RuntimeException(
                        "Cannot set a negative timeout value.");
            }

            KompicsSimulatorEvent timeout = new KompicsSimulatorEvent(event
                    .getTimeoutEvent(), CLOCK + event.getDelay());
            activeTimers.put(event.getTimeoutEvent().getTimeoutId(), timeout);
            futureEventList.scheduleFutureEvent(CLOCK, timeout);
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

            PeriodicSimulatorEvent periodicTimeout = new PeriodicSimulatorEvent(
                    event.getTimeoutEvent(), CLOCK + event.getDelay(), event
                    .getPeriod());
            activePeriodicTimers.put(event.getTimeoutEvent().getTimeoutId(),
                    periodicTimeout);
            futureEventList.scheduleFutureEvent(CLOCK, periodicTimeout);
        }
    };
    Handler<CancelTimeout> handleCT = new Handler<CancelTimeout>() {
        public void handle(CancelTimeout event) {
            UUID timeoutId = event.getTimeoutId();
            logger.debug("CancelTimeout: {}. AT={}", timeoutId, activeTimers
                    .keySet());

            KompicsSimulatorEvent kse = activeTimers.remove(timeoutId);

            if (kse != null) {
                kse.cancel();

                // boolean removed = futureEventList.cancelFutureEvent(CLOCK,
                // kse);
                // if (!removed) {
                // logger.warn("Cannot find timeout {}", event.getTimeoutId());
                // }
            } else {
                // CancelTimeout comes after expiration or previous cancelation 
                logger.warn("Cannot find timeout {}", event.getTimeoutId());
            }
        }
    };
    Handler<CancelPeriodicTimeout> handleCPT = new Handler<CancelPeriodicTimeout>() {
        public void handle(CancelPeriodicTimeout event) {
            UUID timeoutId = event.getTimeoutId();
            logger.debug("CancelPeriodicTimeout: {}. APT={}", timeoutId,
                    activePeriodicTimers.keySet());

            KompicsSimulatorEvent kse = activePeriodicTimers.remove(timeoutId);
            boolean removed = futureEventList.cancelFutureEvent(CLOCK, kse);
            if (!removed) {
                logger.warn("Cannot find periodic timeout {}", event
                        .getTimeoutId());
            }
        }
    };
    Handler<TerminateExperiment> handleTerminate = new Handler<TerminateExperiment>() {
        public void handle(TerminateExperiment event) {
            SimulationTerminatedEvent terminatedEvent = new SimulationTerminatedEvent(
                    CLOCK, 0, false);
            futureEventList.scheduleFutureEvent(CLOCK, terminatedEvent);
        }
    };

    // === intercepted calls related to time
    public long java_lang_System_currentTimeMillis() { // System
        return CLOCK;
    }

    public long java_lang_System_nanoTime() { // System
        return CLOCK * 1000000;
    }

    public void java_lang_Thread_sleep(long millis) { // Thread
        // TODO
        throw new RuntimeException(
                "I cannot simulate sleep without a continuation.");
    }

    public void java_lang_Thread_sleep(long millis, int nanos) { // Thread
        if (nanos != 0) {
            throw new RuntimeException("I can't sleep nanos.");
        }
        java_lang_Thread_sleep(millis);
    }

    public void java_lang_Thread_start() { // Thread
        throw new RuntimeException(
                "You cannot start threads in reproducible simulation mode.");
    }

    // statistics
    private final void logTimeStatistics() {
        long realDuration = System.currentTimeMillis() - simulationStartTime;
        logger.info("========================================================");
        logger.info("Simulated time: {}", durationToString(CLOCK));
        logger.info("Real time: {}", durationToString(realDuration));
        if (CLOCK > realDuration) {
            logger.info("Time compression factor: {}",
                    ((double) CLOCK / realDuration));
        } else {
            logger.info("Time expansion factor: {}",
                    ((double) realDuration / CLOCK));
        }
        logger.info("========================================================");
    }

    public static final String durationToString(long duration) {
        StringBuilder sb = new StringBuilder();
        int ms = 0, s = 0, m = 0, h = 0, d = 0, y = 0;

        ms = (int) (duration % 1000);
        // get duration in seconds
        duration /= 1000;
        s = (int) (duration % 60);
        // get duration in minutes
        duration /= 60;
        if (duration > 0) {
            m = (int) (duration % 60);
            // get duration in hours
            duration /= 60;
            if (duration > 0) {
                h = (int) (duration % 24);
                // get duration in days
                duration /= 24;
                if (duration > 0) {
                    d = (int) (duration % 365);
                    // get duration in years
                    y = (int) (duration / 365);
                }
            }
        }
        boolean printed = false;
        if (y > 0) {
            sb.append(y).append("y ");
            printed = true;
        }
        if (d > 0) {
            sb.append(d).append("d ");
            printed = true;
        }
        if (h > 0) {
            sb.append(h).append("h ");
            printed = true;
        }
        if (m > 0) {
            sb.append(m).append("m ");
            printed = true;
        }
        if (s > 0 || !printed) {
            sb.append(s);
            if (ms > 0) {
                sb.append(".").append(String.format("%03d", ms));
            }
            sb.append("s");
        }
        return sb.toString();
    }
}
