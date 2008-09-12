package se.sics.kompics.core.scheduler;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import se.sics.kompics.core.ComponentCore;

public class Worker extends Thread {

	private final Scheduler scheduler;

	private final int id;

	public AtomicInteger qsize = new AtomicInteger(0);

	private final ConcurrentLinkedQueue<ComponentCore> wq;

	/* === STATS === */
	public int tws, sws, fws, twc;
	public int wc, minwc = Integer.MAX_VALUE, maxwc;
	double avgwc;
	long totwc;
	public int maxQs = 0;
	public Work.Pool pool;

	public int minws = Integer.MAX_VALUE, maxws;
	double avgws;

	public Worker(Scheduler scheduler, int id) {
		super("Worker-" + id);
		this.scheduler = scheduler;
		this.id = id;
		this.wq = new ConcurrentLinkedQueue<ComponentCore>();
		// Work.Pool.set(new Work.Pool());
	}

	public void run() {
		pool = Work.Pool.get();
		while (true) {
			// try to take from the queue
			ComponentCore c = wq.poll();
			if (c != null) {
				// got some work, do it
				doWork(c);
				qsize.decrementAndGet();
			} else {
				// my queue was empty. I'll try to steal some work
				stealMoreWork();
			}
		}
	}

	private void stealOneWork() {
		tws++;
		ComponentCore c = scheduler.stealOneWorkFromHighest(id);
		if (c != null) {
			resetWc();
			wsStat(1);
			sws++;
			// I managed to steal some work, do it
			doWork(c);
		} else {
			// I could not steal any work. I'll sleep for a while
			// System.err.print(".");
			fws++;
			synchronized (this) {
				try {
					this.wait(Scheduler.SLEEP_MS);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	private void stealMoreWork() {
		tws++;
		int stolen = scheduler.stealMoreWorkFromHighest(id);

		if (stolen > 0) {
			resetWc();
			wsStat(stolen);
			sws++;
			// I managed to steal some work, do it
		} else {
			// I could not steal any work. I'll sleep for a while
			// System.err.print(".");
			fws++;
			synchronized (this) {
				try {
					this.wait(Scheduler.SLEEP_MS);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public void addWork(ComponentCore core) {
		wq.offer(core);
		int qs = qsize.incrementAndGet();
		if (maxQs < qs)
			maxQs = qs;
	}

	public ComponentCore takeWork() {
		ComponentCore core = wq.poll();
		if (core != null) {
			qsize.decrementAndGet();
		}
		return core;
	}

	private void doWork(ComponentCore core) {
		wc++;
		twc++;
		scheduler.countReduction();
		core.run(id);
	}

	void wsStat(int stolen) {
		if (minws > stolen)
			minws = stolen;
		if (maxws < stolen)
			maxws = stolen;
		avgws = (avgws * (sws) + stolen) / (sws + 1);
	}

	void resetWc() {
		if (minwc > wc)
			minwc = wc;
		if (maxwc < wc)
			maxwc = wc;
		totwc += wc;
		avgwc = (avgwc * (sws) + wc) / (sws + 1);
		wc = 0;
	}
}
