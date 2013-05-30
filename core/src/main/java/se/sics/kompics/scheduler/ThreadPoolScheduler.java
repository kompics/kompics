package se.sics.kompics.scheduler;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import se.sics.kompics.Component;
import se.sics.kompics.Kompics;
import se.sics.kompics.Scheduler;

public class ThreadPoolScheduler extends Scheduler {

    private final ThreadPoolExecutor threadPool;
    private final ThreadPoolScheduler self;

    public ThreadPoolScheduler() {
        // threadPool = Executors.newCachedThreadPool(new
        // KompicsThreadFactory());
        threadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L,
                TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
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

    static class KompicsThreadFactory implements ThreadFactory {

        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;

        KompicsThreadFactory() {
            namePrefix = "Kompics-worker-";
        }

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
