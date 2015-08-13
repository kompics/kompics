/**
 * This file is part of the Kompics component model runtime.
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
package se.sics.kompics;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component.State;
import se.sics.kompics.Fault.ResolveAction;
import se.sics.kompics.scheduler.ThreadPoolScheduler;
import se.sics.kompics.scheduler.WorkStealingScheduler;

/**
 * The
 * <code>Kompics</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
public final class Kompics {

    public static final long SHUTDOWN_TIMEOUT = 5000;
    public static Logger logger = LoggerFactory.getLogger("Kompics");
    public static AtomicInteger maxNumOfExecutedEvents = new AtomicInteger(1);
    private static boolean on = false;
    private static Scheduler scheduler;
    private static ComponentCore mainCore;
    private static final Kompics obj = new Kompics();
    private static final FaultHandler defaultFaultHandler = new FaultHandler() {

        @Override
        public Fault.ResolveAction handle(Fault f) {
            return ResolveAction.ESCALATE;
        }
    };
    private static FaultHandler faultHandler = defaultFaultHandler;

    public static void setScheduler(Scheduler sched) {
        synchronized (obj) {
            if (on) {
                throw new RuntimeException("Kompics already created");
            }
            scheduler = sched;
        }
    }

    public static Scheduler getScheduler() {
        synchronized (obj) {
            return scheduler;
        }
    }

    public static void setFaultHandler(FaultHandler fh) {
        synchronized (obj) {
            if (on) {
                throw new RuntimeException("Kompics already created");
            }
            faultHandler = fh;
        }
    }

    public static void resetFaultHandler() {
        synchronized (obj) {
            if (on) {
                throw new RuntimeException("Kompics already created");
            }
            faultHandler = defaultFaultHandler;
        }
    }

    public static FaultHandler getFaultHandler() {
        synchronized (obj) {
            return faultHandler;
        }
    }

    public static boolean isOn() {
        synchronized (obj) {
            return on;
        }
    }

    /**
     * Creates the and start.
     *
     * @param main the main
     */
    public static void createAndStart(Class<? extends ComponentDefinition> main) {
        // createAndStart(main, Runtime.getRuntime().availableProcessors());
        createAndStart(main, Init.NONE, 1);
    }

    public static void createAndStart(Class<? extends ComponentDefinition> main, Init initEvent) {
        createAndStart(main, initEvent, 1);
    }

    /**
     * Creates the and start.
     *
     * @param main the main
     * @param workers the workers
     */
    public static void createAndStart(
            Class<? extends ComponentDefinition> main, int workers) {
        createAndStart(main, Init.NONE, workers, 1);
    }

    public static void createAndStart(
            Class<? extends ComponentDefinition> main, Init initEvent, int workers) {
        createAndStart(main, initEvent, workers, 1);
    }

    public static void createAndStart(
            Class<? extends ComponentDefinition> main, int workers, int maxEventExecuteNumber) {
        createAndStart(main, Init.NONE, workers, maxEventExecuteNumber);
    }

    /**
     * Creates the main component and starts it.
     * <p>
     * @param <T>
     *
     * @param main the main
     * @param initEvent
     * @param workers the workers
     * @param maxEventExecuteNumber
     */
    public static <T extends ComponentDefinition> void createAndStart(
            Class<T> main, Init initEvent, int workers, int maxEventExecuteNumber) {
        synchronized (obj) {
            if (on) {
                throw new RuntimeException("Kompics already created");
            }
            on = true;

            if (scheduler == null) {
                // scheduler = new WorkStealingScheduler(workers);
                scheduler = new ThreadPoolScheduler();
            }

            Kompics.maxNumOfExecutedEvents.lazySet(maxEventExecuteNumber);

            try {
                ComponentDefinition mainComponent;
                if (initEvent == Init.NONE) { // NONE instance
                    mainComponent = main.newInstance();
                } else {
                    Constructor<T> constr = main.getConstructor(initEvent.getClass());
                    mainComponent = constr.newInstance(initEvent);
                }
                mainCore = mainComponent.getComponentCore();
                mainCore.setScheduler(scheduler);

                //mainCore.workCount.incrementAndGet();
                // start Main
                ((PortCore<ControlPort>) mainCore.getControl()).doTrigger(
                        Start.event, 0, mainCore);
            } catch (InstantiationException e) {
                throw new RuntimeException("Cannot create main component "
                        + main.getCanonicalName(), e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot create main component "
                        + main.getCanonicalName(), e);
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException("Cannot create main component "
                        + main.getCanonicalName(), ex);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException("Cannot create main component "
                        + main.getCanonicalName(), ex);
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException("Cannot create main component "
                        + main.getCanonicalName(), ex);
            } catch (SecurityException ex) {
                throw new RuntimeException("Cannot create main component "
                        + main.getCanonicalName(), ex);
            }

            scheduler.proceed();
        }
    }

    private Kompics() {
    }

    public static void shutdown() {
        synchronized (obj) {
            if (mainCore != null) {
                if (mainCore.state == State.ACTIVE) {
                    mainCore.control().doTrigger(Kill.event, mainCore.wid, mainCore);
                }
                synchronized (mainCore) { // Potential deadlock spot
                    long start = System.currentTimeMillis();
                    while (mainCore.state != Component.State.PASSIVE && mainCore.state != Component.State.DESTROYED) {
                        try {
                            mainCore.wait(SHUTDOWN_TIMEOUT);
                        } catch (InterruptedException ex) {
                            logger.warn("Failed orderly Kompics shutdown", ex);
                        }
                        if ((System.currentTimeMillis() - start) > SHUTDOWN_TIMEOUT) {
                            logger.warn("Failed to shutdown Kompics in time. Forcing shutdown.");
                            break;
                        }
                    }
                    mainCore.cleanPorts();
                }
            }
            if (scheduler != null) {
                scheduler.shutdown();
            }
            on = false;
            scheduler = null;
            obj.notifyAll();
        }
    }

    public static void forceShutdown() {
        synchronized (obj) {
            if (scheduler != null) {
                scheduler.shutdown();
            }
            on = false;
            scheduler = null;

            obj.notifyAll();
        }
    }

    public static void waitForTermination() throws InterruptedException {
        synchronized (obj) {
            while (on) {
                obj.wait();
            }
        }
    }

    static void handleFault(final Fault f) {
        final FaultHandler fh = faultHandler;
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                ResolveAction ra = fh.handle(f);
                switch (ra) {
                    case RESOLVED:
                        Kompics.logger.info("Fault {} was resolved by user.", f);
                        break;
                    case IGNORE:
                        Kompics.logger.info("Fault {} was declared to be ignored by user. Resuming component...", f);
                        f.source.markSubtreeAs(Component.State.PASSIVE);
                        f.source.control().doTrigger(Start.event, 0, mainCore);
                        break;
                    case DESTROY:
                        Kompics.logger.info("User declared that Fault {} should quit Kompics...", f);
                        Kompics.forceShutdown();
                         {
                            try {
                                Kompics.waitForTermination();
                            } catch (InterruptedException ex) {
                                Kompics.logger.error("Interrupted while waiting for Kompics termination...");
                                System.exit(1);
                            }
                        }
                        Kompics.logger.info("finished quitting Kompics.");
                        break;
                    default:
                        Kompics.logger.info("User declared that Fault {} should quit JVM...", f);
                        System.exit(1);
                }
            }

        });
        t.start();
    }

    /**
     * Log stats.
     */
    public static void logStats() {
        if (scheduler instanceof WorkStealingScheduler) {
            ((WorkStealingScheduler) scheduler).logStats();
        }
    }
}
