package sandbox.se.sics.kompics;

import java.util.concurrent.atomic.AtomicInteger;

public class Worker extends Thread {

	private final Scheduler scheduler;

	private final int wid;

	private final SpinlockQueue<ComponentCore> workQueue;
	
	private final AtomicInteger workCount;
	
	int executionCount, workStealingCount, sleepCount;

	public Worker(Scheduler scheduler, int wid) {
		super();
		this.scheduler = scheduler;
		this.wid = wid;
		this.workQueue = new SpinlockQueue<ComponentCore>();
		this.workCount = new AtomicInteger(0);
		super.setName("Worker-" + wid);
	}

	@Override
	public final void run() {
		Kompics.logger.error("{} started", getName());
		while (true) {
			try {
				executeComponent();
			} finally {
				// run forever
			}
		}
	}

	private final void executeComponent() {
		ComponentCore core = null;
		do {
			// try to do local work
			core = workQueue.poll();
			if (core == null) {
				// try to steal work from other workers
				workStealingCount++;
				core = scheduler.stealWork(wid);
				if (core == null) {
					// there is no work in the system
					sleepCount++;
					scheduler.waitForWork(this);
				}
			}
		} while (core == null);

		executionCount++;
		core.execute(wid);
	}

	final ComponentCore getWork() {
		ComponentCore core = workQueue.poll();
		if (core != null) {
			workCount.decrementAndGet();
		}
		return core;
	}

	final void addWork(ComponentCore core) {
		workQueue.offer(core);
		workCount.incrementAndGet();
	}

	public final int getWorkCount() {
		return workCount.get();
	}
	
	public final int getWid() {
		return wid;
	}
}
