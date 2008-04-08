package se.sics.kompics.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.Priority;
import se.sics.kompics.core.config.ConfigurationException;
import se.sics.kompics.core.sched.ComponentState;
import se.sics.kompics.core.sched.ReadyComponent;
import se.sics.kompics.core.sched.Scheduler;
import se.sics.kompics.core.sched.Work;
import se.sics.kompics.core.sched.WorkQueue;

public class ComponentCore implements Component {

	/**
	 * reference to the component instance implementing the component
	 * functionality, i.e., state and event handlers
	 */
	// private Object handlerObject;
	/* =============== COMPOSITION =============== */

	/**
	 * internal sub-components
	 */
	// private HashSet<ComponentCore> subcomponents;
	/**
	 * internal channels
	 */
	// private HashSet<ChannelCore> subchannels;
	/* =============== CONFIGURATION =============== */

	private HashMap<Class<? extends Event>, Binding> bindings;

	// private HashMap<Class<? extends Event>, Subscription> subscriptions;

	/* =============== SCHEDULING =============== */

	private Scheduler scheduler;

	private ComponentState componentState;
	private int allWorkCounter;
	private int highWorkCounter;
	private int mediumWorkCounter;
	private int lowWorkCounter;
	private Object componentStateLock;

	private HashMap<ChannelCore, WorkQueue> channelWorkQueues;

	private LinkedHashSet<WorkQueue> highWorkQueuePool;
	private LinkedHashSet<WorkQueue> mediumWorkQueuePool;
	private LinkedHashSet<WorkQueue> lowWorkQueuePool;

	// to sync executing thread with publishing thread for pool selection
	private int highPoolCounter;
	private int mediumPoolCounter;
	private int lowPoolCounter;

	public ComponentCore(Scheduler scheduler) {
		super();
		this.bindings = new HashMap<Class<? extends Event>, Binding>();
		// this.subscriptions = new HashMap<Class<? extends Event>,
		// Subscription>();

		this.scheduler = scheduler;
		this.componentState = ComponentState.ASLEEP;
		this.allWorkCounter = 0;
		this.highWorkCounter = 0;
		this.mediumWorkCounter = 0;
		this.lowWorkCounter = 0;
		this.componentStateLock = new Object();

		this.channelWorkQueues = new HashMap<ChannelCore, WorkQueue>();

		this.highWorkQueuePool = new LinkedHashSet<WorkQueue>();
		this.mediumWorkQueuePool = new LinkedHashSet<WorkQueue>();
		this.lowWorkQueuePool = new LinkedHashSet<WorkQueue>();

		this.highPoolCounter = 0;
		this.mediumPoolCounter = 0;
		this.lowPoolCounter = 0;
	}

	public void setHandlerObject(Object handlerObject) {
		// this.handlerObject = handlerObject;
	}

	/* =============== EVENT TRIGGERING =============== */

	/**
	 * triggers an event
	 * 
	 * @param event
	 *            the triggered event
	 */
	public void triggerEvent(Event event) {
		Binding binding = bindings.get(event.getClass());

		if (binding == null)
			throw new ConfigurationException("Event type "
					+ event.getClass().getCanonicalName() + " not bound");

		EventCore eventCore = new EventCore(event, binding.getChannel(),
				Priority.MEDIUM);
		triggerEventCore(eventCore);
	}

	public void triggerEvent(Event event, Priority priority) {
		Binding binding = bindings.get(event.getClass());

		if (priority == null)
			throw new RuntimeException("triggered event with null priority");

		if (binding == null)
			throw new ConfigurationException("Event type "
					+ event.getClass().getCanonicalName() + " not bound");

		EventCore eventCore = new EventCore(event, binding.getChannel(),
				priority);
		triggerEventCore(eventCore);
	}

	public void triggerEvent(Event event, ChannelCore channel) {
		EventCore eventCore = new EventCore(event, channel, Priority.MEDIUM);
		triggerEventCore(eventCore);
	}

	public void triggerEvent(Event event, ChannelCore channel, Priority priority) {
		if (priority == null)
			throw new RuntimeException("triggered event with null priority");

		EventCore eventCore = new EventCore(event, channel, priority);
		triggerEventCore(eventCore);
	}

	private void triggerEventCore(EventCore eventCore) {
		ChannelCore channelCore = eventCore.getChannelCore();
		channelCore.publishEventCore(eventCore);
	}

	/* =============== EVENT SCHEDULING =============== */

	// many publisher threads can call this method but they shall synchronize on
	// the work queue and on the component state lock
	public void handleWork(Work work) {
		WorkQueue workQueue = channelWorkQueues.get(work.getChannelCore());
		workQueue.add(work);

		// we make the component ready, if passive
		synchronized (componentStateLock) {
			allWorkCounter++;
			switch (work.getPriority()) {
			case HIGH:
				highWorkCounter++;
				break;
			case MEDIUM:
				mediumWorkCounter++;
				break;
			case LOW:
				lowWorkCounter++;
				break;
			}

			if (componentState == ComponentState.ASLEEP) {
				componentState = ComponentState.AWAKE;
				scheduler.componentReady(new ReadyComponent(this,
						highWorkCounter, mediumWorkCounter, lowWorkCounter,
						work.getPriority(), null));
			} else {
				scheduler.publishedEvent(work.getPriority());
			}
		}
	}

	/*
	 * only one thread at a time calls this method. causes the component to
	 * execute one event of the given priority. it is guaranteed that the
	 * component has such an event. If an event of the given priority is not
	 * found at the head of a channel queue, a lower priority event is executed,
	 * if one is available, othewise a higer priority one.
	 */
	public void schedule(Priority priority) {
		// pick a work queue, if possible from the given priority pool
		WorkQueue workQueue = pickWorkQueue(priority);

		// take from it
		Work work = workQueue.take();

		// execute the taken work
		EventHandler eventHandler = work.getEventHandler();
		Event event = work.getEventCore().getEvent();
		boolean handled = false;

		// isolate any possible errors or exceptions while executing event
		// handlers and guard methods
		try {
			handled = eventHandler.handleEvent(event);

			// try to execute blocked event handlers until no more possible
			while (handled && hasBlockedEvents()) {
				handled = handleOneBlockedEvent();
			}

		} catch (Throwable throwable) {
			// TODO implement fault handling. e.g. send a fault event on a
			// supervision channel
			throwable.printStackTrace();
		}

		// make the component passive or ready
		synchronized (componentStateLock) {
			allWorkCounter--;
			switch (work.getPriority()) {
			case HIGH:
				highWorkCounter--;
				break;
			case MEDIUM:
				mediumWorkCounter--;
				break;
			case LOW:
				lowWorkCounter--;
				break;
			}

			if (allWorkCounter == 0) {
				componentState = ComponentState.ASLEEP;
				scheduler.executedEvent(work.getPriority());
			} else if (allWorkCounter > 0) {
				componentState = ComponentState.AWAKE;
				scheduler.componentReady(new ReadyComponent(this,
						highWorkCounter, mediumWorkCounter, lowWorkCounter,
						null, work.getPriority()));
			} else {
				throw new RuntimeException("Negative work counter");
			}
		}
	}

	/**
	 * tries to execute one guarded event handler
	 * 
	 * @return <code>true</code> if one blocked event was executed from any
	 *         guarded event handler and <code>false</code> if no blocked
	 *         event could be executed due to no satisfied guard
	 */
	private boolean handleOneBlockedEvent() {
		// TODO finish guarded handlers
		return false;
	}

	private boolean hasBlockedEvents() {
		// TODO finish guarded handlers
		return false;
	}

	/*
	 * synchronized with moveWorkQueue between executing thread (calling
	 * pickWorkQueue) and publisher thread (calling move...)
	 */
	private synchronized WorkQueue pickWorkQueue(Priority priority) {
		if (priority == Priority.MEDIUM) {
			// this component has been scheduled to execute a MEDIUM event, so
			// it must have a MEDIUM event, in a channel that can only be in the
			// MEDIUM or HIGH pools.
			if (mediumPoolCounter > 0) {
				Iterator<WorkQueue> iterator = mediumWorkQueuePool.iterator();
				return iterator.next();
			} else if (highPoolCounter > 0) {
				Iterator<WorkQueue> iterator = highWorkQueuePool.iterator();
				return iterator.next();
			} else {
				throw new RuntimeException(
						"scheduled MEDIUM but both MEDIUM and HIGH pools empty");
			}
		} else if (priority == Priority.HIGH) {
			// this component has been scheduled to execute a HIGH event, so it
			// must have a HIGH event in a channel that can only be in the HIGH
			// pool.
			if (highPoolCounter > 0) {
				Iterator<WorkQueue> iterator = highWorkQueuePool.iterator();
				return iterator.next();
			} else {
				throw new RuntimeException("scheduled HIGH but HIGH pool empty");
			}
		} else if (priority == Priority.LOW) {
			// this component has been scheduled to execute a LOW event, so it
			// must have a LOW event in a channel that can be in the LOW, MEDIUM
			// or HIGH pools.
			if (lowPoolCounter > 0) {
				Iterator<WorkQueue> iterator = lowWorkQueuePool.iterator();
				return iterator.next();
			} else if (highPoolCounter > 0) {
				Iterator<WorkQueue> iterator = highWorkQueuePool.iterator();
				return iterator.next();
			} else if (mediumPoolCounter > 0) {
				Iterator<WorkQueue> iterator = mediumWorkQueuePool.iterator();
				return iterator.next();
			} else {
				throw new RuntimeException("scheduled LOW but all pools empty");
			}
		} else {
			throw new RuntimeException("Bad priority");
		}
	}

	/*
	 * called by the WorkQueue to move itself to the end of the priority pool,
	 * maybe to a different priority pool. Both from and to can be null.
	 */
	public synchronized void moveWorkQueueToPriorityPool(WorkQueue workQueue,
			Priority from, Priority to) {

		// constant-time removal
		switch (from) {
		case LOW:
			lowWorkQueuePool.remove(workQueue);
			lowPoolCounter--;
			break;
		case MEDIUM:
			mediumWorkQueuePool.remove(workQueue);
			mediumPoolCounter--;
			break;
		case HIGH:
			highWorkQueuePool.remove(workQueue);
			highPoolCounter--;
			break;
		}

		// constant-time addition
		switch (to) {
		case LOW:
			lowWorkQueuePool.add(workQueue);
			lowPoolCounter++;
			break;
		case MEDIUM:
			mediumWorkQueuePool.add(workQueue);
			mediumPoolCounter++;
			break;
		case HIGH:
			highWorkQueuePool.add(workQueue);
			highPoolCounter++;
			break;
		}
	}

	/* =============== CONFIGURATION =============== */
}
