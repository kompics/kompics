package se.sics.kompics.core.scheduler;

public class WorkQueue {

	private SpinlockQueue<Work> workQueue;

	public WorkQueue() {
		super();
		this.workQueue = new SpinlockQueue<Work>();
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
