package se.sics.kompics.core.scheduler;

import java.util.concurrent.ConcurrentLinkedQueue;

public class WorkQueue {

	private ConcurrentLinkedQueue<Work> workQueue;

	public WorkQueue() {
		super();
		this.workQueue = new ConcurrentLinkedQueue<Work>();
	}

	/* called by the publisher thread */
	public void add(Work work) {
		workQueue.add(work);
	}

	/* called by the executing thread */
	public Work take() {
		return workQueue.poll();
	}
}
