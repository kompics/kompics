/*
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) 
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
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
package se.sics.kompics.scheduler;

import java.util.concurrent.atomic.AtomicInteger;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentCore;
import se.sics.kompics.Kompics;
import se.sics.kompics.Scheduler;
import se.sics.kompics.SpinlockQueue;

/**
 * The <code>Scheduler</code> class.
 *
 * @author Cosmin Arad {@literal <cosmin@sics.se>}
 * @author Jim Dowling {@literal <jdowling@sics.se>}
 * @version $Id$
 */
public final class WorkStealingScheduler extends Scheduler {

    private final int workerCount;
    private final Worker[] workers;
    private final boolean[] on;
    private final SpinlockQueue<Worker> sleepingWorkers;
    private final AtomicInteger sleepingWorkerCount;

    /**
     * Instantiates a new scheduler.
     *
     * @param wc
     *            the wc
     */
    public WorkStealingScheduler(int wc) {
        workerCount = wc;
        workers = new Worker[workerCount];
        sleepingWorkers = new SpinlockQueue<Worker>();
        sleepingWorkerCount = new AtomicInteger(0);
        on = new boolean[workerCount];

        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker(this, i);
            on[i] = true;
        }
    }

    public final void proceed() {
        for (int i = 0; i < workers.length; i++) {
            workers[i].start();
        }
    }

    public final void shutdown() {
        for (int i = 0; i < workers.length; i++) {
            synchronized (workers[i]) {
                on[i] = false;
                workers[i].quitWhenNoMoreWork();
                workers[i].notify();
            }
        }
    }

    public final void schedule(Component component, int wid) {
        ComponentCore core = (ComponentCore) component;
        // check if any worker need waking up
        int swc = sleepingWorkerCount.get();
        Worker toAwake = null;
        if (swc > 0) {
            swc = sleepingWorkerCount.getAndDecrement();
            if (swc > 0) {
                toAwake = sleepingWorkers.poll();
            }
        }

        if (toAwake == null) {
            // add new work to the queue of the worker who generated it
            workers[wid].addWork(core);
        } else {
            // add new work to the queue of the worker to be awaken
            workers[toAwake.getWid()].addWork(core);

            // wake up sleeping worker
            synchronized (toAwake) {
                toAwake.notify();
            }
        }

    }

    final ComponentCore stealWork(int wid) {
        ComponentCore core = null;
        int wmax, max;
        do {
            max = 0;
            wmax = wid;
            for (int i = 0; i < workers.length; i++) {
                if (i != wid) {
                    int wc = workers[i].getWorkCount();
                    if (wc > max) {
                        max = wc;
                        wmax = i;
                    }
                }
            }
            core = workers[wmax].getWork();
            // repeat until some worker has some work
        } while (core == null && max > 0);
        return core;
    }

    final void waitForWork(Worker w) {
        synchronized (w) {
            sleepingWorkers.offer(w);
            sleepingWorkerCount.incrementAndGet();
            // try {
            // // Kompics.logger.debug("{} sleeping.", w.getWid());
            // if (!on[w.getWid()]) {
            // // do not wait when the worker is supposed to quit
            // return;
            // }
            //
            // w.wait();
            // } catch (InterruptedException e) {
            // }
            // Kompics.logger.debug("{} sleeping.", w.getWid());
            if (!on[w.getWid()]) {
                // do not wait when the worker is supposed to quit
                return;
            }
            w.waitForWork();
            // Kompics.logger.debug("{} woke up.", w.getWid());
        }
    }

    public final void logStats() {
        int ex = 0, ws = 0, sl = 0;
        for (int i = 0; i < workers.length; i++) {
            ex += workers[i].executionCount;
            ws += workers[i].workStealingCount;
            sl += workers[i].sleepCount;
            Kompics.logger.error("Worker {}: executed {}, stole {}, slept {}",
                    new Object[] { i, workers[i].executionCount, workers[i].workStealingCount, workers[i].sleepCount });
        }
        Kompics.logger.error("TOTAL: executed {}, stole {}, slept {}", new Object[] { ex, ws, sl });
    }

    final void execute(Component component, int w) {
        executeComponent(component, w);
    }

    @Override
    public void asyncShutdown() {
        shutdown();
    }
}
