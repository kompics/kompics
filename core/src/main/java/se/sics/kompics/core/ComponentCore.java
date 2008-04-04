package se.sics.kompics.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicInteger;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.Priority;
import se.sics.kompics.core.config.ConfigurationException;
import se.sics.kompics.core.sched.ComponentState;
import se.sics.kompics.core.sched.Work;
import se.sics.kompics.core.sched.WorkQueue;

public class ComponentCore implements Component {

	/**
	 * reference to the component instance implementing the component
	 * functionality, i.e., state and event handlers
	 */
	private Object behaviour;

	/* =============== COMPOSITION =============== */

	/**
	 * internal sub-components
	 */
	private HashSet<ComponentCore> subcomponents;

	/**
	 * internal channels
	 */
	private HashSet<ChannelCore> subchannels;

	/* =============== CONFIGURATION =============== */

	private HashMap<Class<? extends Event>, Binding> bindings;

	private HashMap<Class<? extends Event>, Subscription> subscriptions;

	/* =============== SCHEDULING =============== */

	private ComponentState componentState;
	private AtomicInteger workCounter;

	private HashMap<ChannelCore, WorkQueue> channelWorkQueues;

	private LinkedHashSet<WorkQueue> highWorkQueuePool;
	private LinkedHashSet<WorkQueue> mediumWorkQueuePool;
	private LinkedHashSet<WorkQueue> lowWorkQueuePool;

	// to sync executing thread with publishing thread for pool selection
	private int highPoolCounter;
	private int mediumPoolCounter;
	private int lowPoolCounter;

	public ComponentCore() {
		super();
		bindings = new HashMap<Class<? extends Event>, Binding>();
		subscriptions = new HashMap<Class<? extends Event>, Subscription>();

		componentState = ComponentState.PASSIVE;
		workCounter = new AtomicInteger(0);

		channelWorkQueues = new HashMap<ChannelCore, WorkQueue>();

		highWorkQueuePool = new LinkedHashSet<WorkQueue>();
		mediumWorkQueuePool = new LinkedHashSet<WorkQueue>();
		lowWorkQueuePool = new LinkedHashSet<WorkQueue>();

		highPoolCounter = 0;
		mediumPoolCounter = 0;
		lowPoolCounter = 0;
	}

	public void setBehaviour(Object behaviour) {
		this.behaviour = behaviour;
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

	/* =============== SCHEDULING =============== */

	// many publisher threads can call this method but they shall synchronize on
	// the work queue
	public void handleWork(Work work) {
		WorkQueue workQueue = channelWorkQueues.get(work.getChannelCore());
		workQueue.add(work);

		// we make the component ready
	}

	/* only one thread at a time calls this method */
	public void schedule(Priority priority) {
		// pick a work queue
		WorkQueue workQueue = pickWorkQueue();

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
		;
	}

	/**
	 * tries to execute one guarded event handler
	 * 
	 * @return <code>true</code> if one blocked event was executed from any
	 *         guarded event handler and <code>false</code> if no blocked
	 *         event could be executed due to no satisfied guard
	 */
	private boolean handleOneBlockedEvent() {
		// TODO
		return false;
	}

	private boolean hasBlockedEvents() {
		// TODO
		return false;
	}

	private synchronized WorkQueue pickWorkQueue() {
		WorkQueue workQueue;
		if (highPoolCounter > 0) {
			Iterator<WorkQueue> iterator = highWorkQueuePool.iterator();
			workQueue = iterator.next();
		} else if (mediumPoolCounter > 0) {
			Iterator<WorkQueue> iterator = mediumWorkQueuePool.iterator();
			workQueue = iterator.next();
		} else if (mediumPoolCounter > 0) {
			Iterator<WorkQueue> iterator = lowWorkQueuePool.iterator();
			workQueue = iterator.next();
		} else {
			throw new RuntimeException("All work queue pools are empty");
		}
		return workQueue;
	}

	/*
	 * called by the WorkQueue to move itself, maybe to a different priority
	 * pool. Both from and to can be null.
	 */
	public synchronized void moveWorkQueueToPriorityPool(WorkQueue workQueue,
			Priority from, Priority to) {
		if (from == to) {
			return;
		}

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
