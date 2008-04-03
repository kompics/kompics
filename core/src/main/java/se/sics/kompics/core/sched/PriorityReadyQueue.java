package se.sics.kompics.core.sched;

import se.sics.kompics.api.Priority;
import se.sics.kompics.core.ComponentCore;

/**
 * 
 * This priority ready queue is a queue of ready components. The components are
 * queued in the order in which they become ready, in three different priority
 * levels.
 * 
 * @author Cosmin Arad
 * 
 */
public class PriorityReadyQueue {

	/**
	 * @return
	 */
	public synchronized ComponentCore take() {
		return null;
	}

	
	/**
	 * Makes a component ready, by adding it to the ready queue.
	 * This method should be called when a new work item is created for this 
	 * 
	 * @param component
	 * @param priority
	 */
	public synchronized void ready(ComponentCore component,
			Priority priority) {

	}
}
