package se.sics.kompics.core.sched;

import java.util.Iterator;
import java.util.LinkedHashSet;

import se.sics.kompics.api.Priority;

/**
 * 
 * This priority ready queue is a queue of ready components. The components are
 * queued in the order in which they become ready, in three different priority
 * levels.
 * 
 * @author Cosmin Arad
 * 
 */
public class FairPriorityReadyQueue {

	private int fairnessRate;

	private int waitingHigh;
	private int waitingMedium;
	private int waitingLow;

	private int readyHigh;
	private int readyMedium;
	private int readyLow;

	private int executedHigh;
	private int executedMedium;

	private LinkedHashSet<ReadyComponent> highQueue;
	private LinkedHashSet<ReadyComponent> mediumQueue;
	private LinkedHashSet<ReadyComponent> lowQueue;

	public FairPriorityReadyQueue(int fairnessRate) {
		super();
		this.fairnessRate = fairnessRate;
		this.waitingHigh = 0;
		this.waitingMedium = 0;
		this.waitingLow = 0;
		this.readyHigh = 0;
		this.readyMedium = 0;
		this.readyLow = 0;
		this.executedHigh = 0;
		this.executedMedium = 0;
		this.highQueue = new LinkedHashSet<ReadyComponent>();
		this.mediumQueue = new LinkedHashSet<ReadyComponent>();
		this.lowQueue = new LinkedHashSet<ReadyComponent>();
	}

	/* =============== PUTTING INTO THE QUEUE =============== */

	/**
	 * Makes a component ready, by adding it to the ready queue. This method is
	 * called when a new work item is created for this component, and when the
	 * component becomes ready to execute another ready event after completing
	 * the execution of some event.
	 * 
	 * @param component
	 * @param priority
	 */
	public synchronized void put(ReadyComponent readyComponent) {
		// add the component to all eligible queues and increment global
		// counters of ready events
		if (readyComponent.hasHighEventsReady()) {
			highQueue.add(readyComponent);
			readyHigh += readyComponent.getHighReadyEventCount();
		}
		if (readyComponent.hasMediumEventsReady()) {
			mediumQueue.add(readyComponent);
			readyMedium += readyComponent.getMediumReadyEventCount();
		}
		if (readyComponent.hasLowEventsReady()) {
			lowQueue.add(readyComponent);
			readyLow += readyComponent.getLowReadyEventCount();
		}
		// component became ready as a result of a new event being published
		if (readyComponent.getPublishedPriority() != null) {
			privatePublishedEvent(readyComponent.getPublishedPriority());
		}
		// component became ready after completing the execution of some event
		if (readyComponent.getExecutedPriority() != null) {
			privateExecutedEvent(readyComponent.getExecutedPriority());
		}
	}

	/**
	 * an event was published for an AWAKE component
	 * 
	 * @param priority
	 */
	public synchronized void publishedEvent(Priority priority) {
		privatePublishedEvent(priority);
	}

	public synchronized void executedEvent(Priority priority) {
		privateExecutedEvent(priority);
	}

	private void privatePublishedEvent(Priority priority) {
		switch (priority) {
		case MEDIUM:
			waitingMedium++;
			break;
		case HIGH:
			waitingHigh++;
			break;
		case LOW:
			waitingLow++;
			break;
		default:
			throw new RuntimeException("Bad priority");
		}
	}

	private void privateExecutedEvent(Priority priority) {
		switch (priority) {
		case MEDIUM:
			waitingMedium--;
			if (waitingLow > 0) {
				executedMedium++;
			}
			if (executedMedium < fairnessRate) {
				// allow HIGH after MEDIUM only if MEDIUM fairness not exceeded,
				// otherwise LOW
				executedHigh = 0;
			}
			break;
		case HIGH:
			waitingHigh--;
			if (waitingMedium > 0 || waitingLow > 0) {
				executedHigh++;
			}
			break;
		case LOW:
			waitingLow--;
			executedHigh = 0;
			executedMedium = 0;
			break;
		default:
			throw new RuntimeException("Bad priority");
		}
	}

	/* =============== TAKING FROM THE QUEUE =============== */

	/**
	 * takes a component from the ready components, according to the priorities
	 * of the ready components and fairness constraints.
	 * 
	 * @return
	 */
	public synchronized ReadyComponent take() {
		ReadyComponent readyComponent = null;

		if (readyHigh > 0) {
			if (executedHigh < fairnessRate) {
				// I have HIGH and HIGH fairness not exceeded
				readyComponent = removeFirst(highQueue, Priority.HIGH);
			} else if (readyMedium > 0) {
				// HIGH fairness exceeded and I have MEDIUM
				if (executedMedium < fairnessRate) {
					// I have MEDIUM and MEDIUM fairness not exceeded
					readyComponent = removeFirst(mediumQueue, Priority.MEDIUM);
				} else if (readyLow > 0) {
					// MEDIUM fairness exceeded and I have LOW
					readyComponent = removeFirst(lowQueue, Priority.LOW);
				} else {
					// MEDIUM fairness exceeded and I only have MEDIUM and HIGH
					throw new RuntimeException("impossible");
				}
			} else if (readyLow > 0) {
				// HIGH fairness exceeded and I have only LOW
				readyComponent = removeFirst(lowQueue, Priority.LOW);
			} else {
				// HIGH fairness exceeded but I only have HIGH
				throw new RuntimeException("impossible");
			}
		} else if (readyMedium > 0) {
			// I have no HIGH, but I have MEDIUM
			if (executedMedium < fairnessRate) {
				// I have MEDIUM and MEDIUM fairness not exceeded
				readyComponent = removeFirst(mediumQueue, Priority.MEDIUM);
			} else if (readyLow > 0) {
				// MEDIUM fairness exceeded and I have LOW
				readyComponent = removeFirst(lowQueue, Priority.LOW);
			} else {
				// MEDIUM fairness exceeded and I only have MEDIUM
				throw new RuntimeException("impossible");
			}
		} else if (readyLow > 0) {
			// I only have LOW
			readyComponent = removeFirst(lowQueue, Priority.LOW);
		} else {
			// I have no HIGH, MEDIUM, or LOW
			throw new RuntimeException("empty");
		}

		takenComponent(readyComponent);
		return readyComponent;
	}

	/**
	 * removes the first ready component from the given queue, and also removes
	 * that same component from other queues it may be part of.
	 * 
	 * @param queue
	 * @param priority
	 * @return
	 */
	private ReadyComponent removeFirst(LinkedHashSet<ReadyComponent> queue,
			Priority priority) {
		// get the first component out of the given queue
		Iterator<ReadyComponent> iterator = queue.iterator();
		ReadyComponent readyComponent = iterator.next();

		// remove the component from all eligible queues
		if (readyComponent.hasHighEventsReady()) {
			highQueue.remove(readyComponent);
		}
		if (readyComponent.hasMediumEventsReady()) {
			mediumQueue.remove(readyComponent);
		}
		if (readyComponent.hasLowEventsReady()) {
			lowQueue.remove(readyComponent);
		}

		// this components will execute a ready event of the specified priority
		// and it is guaranteed to have such a ready event
		readyComponent.setScheduledPriority(priority);
		return readyComponent;
	}

	private void takenComponent(ReadyComponent readyComponent) {
		// decrement ready event counters
		readyHigh -= readyComponent.getHighReadyEventCount();
		readyMedium -= readyComponent.getMediumReadyEventCount();
		readyLow -= readyComponent.getLowReadyEventCount();
	}
}
