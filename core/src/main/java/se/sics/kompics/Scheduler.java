package se.sics.kompics;

import java.util.concurrent.atomic.AtomicInteger;

public final class Scheduler {

	public static Scheduler scheduler;

	private final int workerCount;

	private final Worker[] workers;

	private final SpinlockQueue<Worker> sleepingWorkers;
	private final AtomicInteger sleepingWorkerCount;

	public Scheduler(int wc) {
		workerCount = wc;
		workers = new Worker[workerCount];
		sleepingWorkers = new SpinlockQueue<Worker>();
		sleepingWorkerCount = new AtomicInteger(0);

		for (int i = 0; i < workers.length; i++) {
			workers[i] = new Worker(this, i);
		}
	}

	final void start() {
		Kompics.logger.error("Starting {} workers.", workers.length);

		for (int i = 0; i < workers.length; i++) {
			workers[i].start();
		}
	}

	final void schedule(ComponentCore core, int wid) {
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
		int wmax = wid, max = 0;
		do {
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
			try {
				Kompics.logger.debug("{} sleeping.", w.getWid());
				w.wait();
			} catch (InterruptedException e) {
			}
			Kompics.logger.debug("{} woke up.", w.getWid());
		}
	}

	final void logStats() {
		int ex = 0, ws = 0, sl = 0;
		for (int i = 0; i < workers.length; i++) {
			ex += workers[i].executionCount;
			ws += workers[i].workStealingCount;
			sl += workers[i].sleepCount;
			Kompics.logger
					.error("Worker {}: executed {}, stole {}, slept {}",
							new Object[] { i, workers[i].executionCount,
									workers[i].workStealingCount,
									workers[i].sleepCount });
		}
		Kompics.logger.error("TOTAL: executed {}, stole {}, slept {}",
				new Object[] { ex, ws, sl });
	}
}
