/*
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) 
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * This program is free software; you can redistribute it and/or
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
package se.sics.kompics.scheduler;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;
import se.sics.kompics.Component;
import se.sics.kompics.Fault;
import se.sics.kompics.Kompics;
import se.sics.kompics.Scheduler;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class ForkJoinScheduler extends Scheduler {

    private final ForkJoinPool pool;

    public ForkJoinScheduler(int workers) {
        pool = new ForkJoinPool(workers, new KompicsThreadFactory(), new KompicsUncaughtExceptionHandler(), true);
    }

    @Override
    public void schedule(Component c, int w) {
        pool.execute((ForkJoinTask<Void>) c);
    }

    @Override
    public void proceed() {
        // Do nothing
    }

    @Override
    public void shutdown() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(Kompics.SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS)) {
                Kompics.logger.warn("Failed orderly Kompics shutdown");
            }
        } catch (InterruptedException ex) {
            Kompics.logger.warn("Failed orderly Kompics shutdown", ex);
        }
    }

    @Override
    public void asyncShutdown() {
        pool.shutdown();
    }

    static class KompicsUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            Kompics.getFaultHandler().handle(new Fault(e, null, null));
        }

    }

    static class KompicsThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {

        final String namePrefix = "Kompics-worker-";

        @Override
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            worker.setName(namePrefix + worker.getPoolIndex());
            return worker;
        }
    }
}
