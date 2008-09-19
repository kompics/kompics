package se.sics.kompics.core.scheduler;

public class WorkQueue {

	private KompicsQueue<Work> workQueue;

	public WorkQueue() {
		super();
		this.workQueue = new KompicsQueue<Work>();
	}

	/* called by the publisher thread */
	public void add(Work work) {
		workQueue.offer(work);
	}

	/* called by the executing thread */
	public Work take() {
		return workQueue.poll();
	}
}
