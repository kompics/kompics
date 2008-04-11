package se.sics.kompics.core.scheduler;

import java.util.LinkedList;

import se.sics.kompics.api.Priority;
import se.sics.kompics.core.ComponentCore;

public class WorkQueue {

	private LinkedList<Work> workQueue;

	private Priority priority;

	private int highCounter;

	private int mediumCounter;

	private int lowCounter;

	private ComponentCore componentCore;

	public WorkQueue(ComponentCore componentCore) {
		super();
		this.workQueue = new LinkedList<Work>();
		this.highCounter = 0;
		this.mediumCounter = 0;
		this.lowCounter = 0;
		this.priority = null;

		this.componentCore = componentCore;
	}

	/*
	 * add() and take() lock the work queue. they have to lock the componentCore
	 * for moving the workQueue to a different priority pool, except for the
	 * case where the priority pool does not change (common case). For this
	 * reason we do not grab the componentCore lock directly, but only lock the
	 * workQueue first. This may result in grabbing 2 locks in some cases but
	 * improves parallelism in the common case.
	 */

	/* called by the publisher thread */
	public synchronized void add(Work work) {
		workQueue.add(work);
		incrementCounter(work.getPriority());
		if (priority == null) {
			priority = work.getPriority();
			componentCore.moveWorkQueueToPriorityPool(this, null, priority);
			return;
		}

		if (priority.compareTo(work.getPriority()) < 0) {
			componentCore.moveWorkQueueToPriorityPool(this, priority, work
					.getPriority());
			priority = work.getPriority();
		}
	}

	/* called by the executing thread */
	public synchronized Work take() {
		Work work = workQueue.poll();
		decrementCounter(work.getPriority());

		if (lowCounter + mediumCounter + highCounter == 0) {
			// if all counters are 0, remove work queue from any pool
			componentCore.moveWorkQueueToPriorityPool(this, priority, null);
			priority = null;
			return work;
		}

		if (highCounter == 0 && mediumCounter > 0) {
			// if we have medium works but no high work, move to the medium pool
			componentCore.moveWorkQueueToPriorityPool(this, priority,
					Priority.MEDIUM);
			priority = Priority.MEDIUM;
		}

		if (highCounter > 0) {
			// if we have high works, move to the high pool
			componentCore.moveWorkQueueToPriorityPool(this, priority,
					Priority.HIGH);
			priority = Priority.HIGH;
			return work;
		}

		// we have only low works, move to the low pool
		componentCore.moveWorkQueueToPriorityPool(this, priority, Priority.LOW);
		priority = Priority.LOW;
		return work;
	}

	private void incrementCounter(Priority priority) {
		switch (priority) {
		case MEDIUM:
			mediumCounter++;
			break;
		case HIGH:
			highCounter++;
			break;
		case LOW:
			lowCounter++;
			break;
		}
	}

	private void decrementCounter(Priority priority) {
		switch (priority) {
		case MEDIUM:
			mediumCounter--;
			break;
		case HIGH:
			highCounter--;
			break;
		case LOW:
			lowCounter--;
			break;
		}
	}
}
