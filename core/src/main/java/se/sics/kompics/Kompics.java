/*
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
import se.sics.kompics.config.Config;
import se.sics.kompics.config.TypesafeConfig;
import se.sics.kompics.scheduler.ForkJoinScheduler;
import se.sics.kompics.scheduler.WorkStealingScheduler;

/**
 * The <code>Kompics</code> class.
 *
 * @author Cosmin Arad {@literal <cosmin@sics.se>}
 * @author Jim Dowling {@literal <jdowling@sics.se>}
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 * @version $Id$
 */
public final class Kompics {

    public static final long SHUTDOWN_TIMEOUT = 5000;
    public static final Logger logger = LoggerFactory.getLogger("Kompics");
    public static final AtomicInteger maxNumOfExecutedEvents = new AtomicInteger(1);
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
    private static Config config;

    /**
     * Set the Kompics runtime scheduler.
     * 
     * @param sched
     *            the scheduler to set
     * 
     * @throws java.lang.RuntimeException
     *             if the Kompics runtime is already running
     */
    public static void setScheduler(Scheduler sched) {
        synchronized (obj) {
            if (on) {
                throw new RuntimeException("Kompics already created");
            }
            scheduler = sched;
        }
    }

    /**
     * Get the current Kompics runtime scheduler.
     * 
     * @return the current scheduler
     */
    public static Scheduler getScheduler() {
        synchronized (obj) {
            return scheduler;
        }
    }

    /**
     * Set the root fault handler to its default.
     * 
     * @param fh
     *            the new root fault handler
     * 
     * @throws java.lang.RuntimeException
     *             if the Kompics runtime is already running
     */
    public static void setFaultHandler(FaultHandler fh) {
        synchronized (obj) {
            if (on) {
                throw new RuntimeException("Kompics already created");
            }
            faultHandler = fh;
        }
    }

    /**
     * Reset the root fault handler to its default.
     * 
     * @throws java.lang.RuntimeException
     *             if the Kompics runtime is already running
     */
    public static void resetFaultHandler() {
        synchronized (obj) {
            if (on) {
                throw new RuntimeException("Kompics already created");
            }
            faultHandler = defaultFaultHandler;
        }
    }

    /**
     * Get the root fault handler.
     * 
     * @return the root fault handler
     */
    public static FaultHandler getFaultHandler() {
        synchronized (obj) {
            return faultHandler;
        }
    }

    /**
     * Set the root config to the default value.
     * 
     * @param conf
     *            the new config to set
     * 
     * @throws java.lang.RuntimeException
     *             if the Kompics runtime is already running
     */
    public static void setConfig(Config conf) {
        synchronized (obj) {
            if (on) {
                throw new RuntimeException("Kompics already created");
            }
            config = conf;
        }
    }

    /**
     * Reset the root config to the default value.
     * 
     * @throws java.lang.RuntimeException
     *             if the Kompics runtime is already running
     */
    public static void resetConfig() {
        synchronized (obj) {
            if (on) {
                throw new RuntimeException("Kompics already created");
            }
            config = TypesafeConfig.load();
        }
    }

    /**
     * Get the top-level config instance.
     * 
     * Will load a default config, if none was set before.
     * 
     * @return the root config
     */
    public static Config getConfig() {
        synchronized (obj) {
            if (config == null) {
                config = TypesafeConfig.load();
            }
            return config;
        }
    }

    /**
     * Checks if the Kompics runtime is currently running.
     * 
     * @return true, if its running
     */
    public static boolean isOn() {
        synchronized (obj) {
            return on;
        }
    }

    /**
     * Creates the top-level component and starts it together with the Kompics runtime.
     * 
     * The value {@code Init.NONE} is passed to the root component.
     *
     * @param <C>
     *            the type of the root component
     * @param main
     *            the class instance of the root component
     */
    @SuppressWarnings("unchecked")
    public static <C extends ComponentDefinition> void createAndStart(Class<C> main) {
        // createAndStart(main, Runtime.getRuntime().availableProcessors());
        createAndStart(main, (Init<C>) Init.NONE, 1);
    }

    /**
     * Creates the top-level component and starts it together with the Kompics runtime.
     * 
     * Passes the provided init event to the root component.
     *
     * @param <C>
     *            the type of the root component
     * @param main
     *            the class instance of the root component
     * @param initEvent
     *            the init event to pass to the root component
     */
    public static <C extends ComponentDefinition> void createAndStart(Class<C> main, Init<C> initEvent) {
        createAndStart(main, initEvent, 1);
    }

    /**
     * Creates the top-level component and starts it together with the Kompics runtime.
     * 
     * The value {@code Init.NONE} is passed to the root component.
     *
     * @param <C>
     *            the type of the root component
     * @param main
     *            the class instance of the root component
     * @param workers
     *            the number of workers to use in the runtime's threadpool
     */
    @SuppressWarnings("unchecked")
    public static <C extends ComponentDefinition> void createAndStart(Class<C> main, int workers) {
        createAndStart(main, (Init<C>) Init.NONE, workers, 1);
    }

    /**
     * Creates the top-level component and starts it together with the Kompics runtime.
     * 
     * The value {@code Init.NONE} is passed to the root component.
     *
     * @param <C>
     *            the type of the root component
     * @param main
     *            the class instance of the root component
     * @param initEvent
     *            the init event to pass to the root component
     * @param workers
     *            the number of workers to use in the runtime's threadpool
     */
    public static <C extends ComponentDefinition> void createAndStart(Class<C> main, Init<C> initEvent, int workers) {
        createAndStart(main, initEvent, workers, 1);
    }

    /**
     * Creates the top-level component and starts it together with the Kompics runtime.
     * 
     * The value {@code Init.NONE} is passed to the root component.
     *
     * @param <C>
     *            the type of the root component
     * @param main
     *            the class instance of the root component
     * @param workers
     *            the number of workers to use in the runtime's threadpool
     * @param maxEventExecuteNumber
     *            the maximum number of events to execute before forcing a rescheduling of a component
     */
    @SuppressWarnings("unchecked")
    public static <C extends ComponentDefinition> void createAndStart(Class<C> main, int workers,
            int maxEventExecuteNumber) {
        createAndStart(main, (Init<C>) Init.NONE, workers, maxEventExecuteNumber);
    }

    /**
     * Creates the top-level component and starts it together with the Kompics runtime.
     * 
     * The value {@code Init.NONE} is passed to the root component.
     *
     * @param <C>
     *            the type of the root component
     * @param main
     *            the class instance of the root component
     * @param initEvent
     *            the init event to pass to the root component
     * @param workers
     *            the number of workers to use in the runtime's threadpool
     * @param maxEventExecuteNumber
     *            the maximum number of events to execute before forcing a rescheduling of a component
     */
    public static <C extends ComponentDefinition> void createAndStart(Class<C> main, Init<C> initEvent, int workers,
            int maxEventExecuteNumber) {
        synchronized (obj) {
            if (on) {
                throw new RuntimeException("Kompics already created");
            }
            on = true;

            if (scheduler == null) {
                // scheduler = new WorkStealingScheduler(workers);
                scheduler = new ForkJoinScheduler(workers);
                // scheduler = new ThreadPoolScheduler(workers);
            }

            Kompics.maxNumOfExecutedEvents.lazySet(maxEventExecuteNumber);

            try {
                ComponentDefinition mainComponent;
                if (initEvent == Init.NONE) { // NONE instance
                    mainComponent = main.newInstance();
                } else {
                    Constructor<C> constr = main.getConstructor(initEvent.getClass());
                    mainComponent = constr.newInstance(initEvent);
                }
                mainCore = mainComponent.getComponentCore();
                mainCore.setScheduler(scheduler);

                // mainCore.workCount.incrementAndGet();
                // start Main
                ((PortCore<ControlPort>) mainCore.getControl()).doTrigger(Start.event, 0, mainCore);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                throw new RuntimeException("Cannot create main component " + main.getCanonicalName(), e);
            }

            scheduler.proceed();
        }
    }

    private Kompics() {
    }

    /**
     * Asynchronously shut down the Kompics runtime.
     * <p>
     * Can be used safely from within components.
     */
    public static void asyncShutdown() {
        Runnable r = new Runnable() {

            @Override
            public void run() {
                Kompics.shutdown();
                ;
            }
        };
        Thread t = new Thread(r);
        t.start();
    }

    /**
     * Synchronously shut down the Kompics runtime.
     * <p>
     * Can <b>not</b> be used safely from within Kompics components. Use {@link #asyncShutdown()} instead.
     */
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
                mainCore.cleanPorts();
            }
            if (scheduler != null) {
                scheduler.shutdown();
            }
            on = false;
            scheduler = null;
            obj.notifyAll();
        }
    }

    /**
     * Force an immediate shutdown of the Kompics runtime.
     * <p>
     * Can <b>not</b> be used safely from within Kompics components.
     */
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

    /**
     * Wait until the Kompics runtime is shut down.
     * <p>
     * Can <b>not</b> be used safely from within Kompics components.
     * 
     * @throws InterruptedException
     *             if the waiting is interrupted
     */
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
                    Kompics.forceShutdown(); {
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
     * Log statistics if the @{link WorkStealingScheduler} is used.
     * 
     * @deprecated since 1.2.0, don't use the the @{link WorkStealingScheduler}!
     */
    @Deprecated
    public static void logStats() {
        if (scheduler instanceof WorkStealingScheduler) {
            ((WorkStealingScheduler) scheduler).logStats();
        }
    }
}
