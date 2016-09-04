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
package se.sics.kompics.scheduler;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import se.sics.kompics.Component;
import se.sics.kompics.Kompics;
import se.sics.kompics.Scheduler;

public class ThreadPoolScheduler extends Scheduler {

    private final ThreadPoolExecutor threadPool;
    private final ThreadPoolScheduler self;

    public ThreadPoolScheduler(int workers) {
        threadPool = new ThreadPoolExecutor(workers, workers, 60L,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                new KompicsThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
        self = this;
    }

    @Override
    public void schedule(final Component c, int w) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                self.executeComponent(c, 0);
            }
        });
    }

    @Override
    public void proceed() {
    }

    @Override
    public void shutdown() {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(Kompics.SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS)) {
                Kompics.logger.warn("Failed orderly Kompics shutdown");
            }
        } catch (InterruptedException ex) {
            Kompics.logger.warn("Failed orderly Kompics shutdown", ex);
        }
    }

    @Override
    public void asyncShutdown() {
        threadPool.shutdown();
    }

    static class KompicsThreadFactory implements ThreadFactory {

        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;

        KompicsThreadFactory() {
            namePrefix = "Kompics-worker-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix
                    + threadNumber.getAndIncrement());
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}
