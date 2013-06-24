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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    // TODO Deal with unneeded drop warning/implement needSet.
    // TODO port scheduler including Free List and spin-lock queue.
    // TODO BUG in execution PortCore.pickWork() returns null.
    public static Logger logger = LoggerFactory.getLogger("Kompics");
    public static AtomicInteger maxNumOfExecutedEvents = new AtomicInteger(1);
    private static boolean on = false;
    private static Scheduler scheduler;
    private static ComponentCore mainCore;
    private static final Kompics obj = new Kompics();

    public static void setScheduler(Scheduler sched) {
        scheduler = sched;
    }

    public static Scheduler getScheduler() {
        return scheduler;
    }

    public static boolean isOn() {
        return on;
    }

    /**
     * Creates the and start.
     *
     * @param main the main
     */
    public static void createAndStart(Class<? extends ComponentDefinition> main) {
        // createAndStart(main, Runtime.getRuntime().availableProcessors());
        createAndStart(main, 1);
    }

    /**
     * Creates the and start.
     *
     * @param main the main
     * @param workers the workers
     */
    public static void createAndStart(
            Class<? extends ComponentDefinition> main, int workers) {
        createAndStart(main, workers, 1);
    }

    /**
     * Creates the and start.
     *
     * @param main the main
     * @param workers the workers
     */
    public static void createAndStart(
            Class<? extends ComponentDefinition> main, int workers, int maxEventExecuteNumber) {
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
            ComponentDefinition mainComponent = main.newInstance();
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
        }

        scheduler.proceed();
    }

    private Kompics() {
    }

    public static void shutdown() {
        if (mainCore != null) {
            mainCore.control().doTrigger(Stop.event, mainCore.wid, mainCore);
            synchronized (mainCore) {
                long start = System.currentTimeMillis();
                while (mainCore.state != Component.State.PASSIVE) {
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
            }
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
        on = false;
        scheduler = null;
        synchronized (obj) {
            obj.notifyAll();
        }
    }

    public static void forceShutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        on = false;
        scheduler = null;
        synchronized (obj) {
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

    /**
     * Log stats.
     */
    public static void logStats() {
        if (scheduler instanceof WorkStealingScheduler) {
            ((WorkStealingScheduler) scheduler).logStats();
        }
    }
}
