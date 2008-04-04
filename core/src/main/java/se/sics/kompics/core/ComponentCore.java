package se.sics.kompics.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.Priority;
import se.sics.kompics.core.config.ConfigurationException;
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

	private HashMap<ChannelCore, WorkQueue> channelWorkQueues;

	private LinkedHashSet<WorkQueue> highWorkQueuePool;
	private LinkedHashSet<WorkQueue> mediumWorkQueuePool;
	private LinkedHashSet<WorkQueue> lowWorkQueuePool;

	public ComponentCore() {
		super();
		bindings = new HashMap<Class<? extends Event>, Binding>();
		subscriptions = new HashMap<Class<? extends Event>, Subscription>();

		channelWorkQueues = new HashMap<ChannelCore, WorkQueue>();

		highWorkQueuePool = new LinkedHashSet<WorkQueue>();
		mediumWorkQueuePool = new LinkedHashSet<WorkQueue>();
		lowWorkQueuePool = new LinkedHashSet<WorkQueue>();
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

	public void handleWork(Work work) {
		WorkQueue workQueue = channelWorkQueues.get(work.getChannelCore());
		workQueue.add(work);

		// we make the component ready
	}

	/*
	 * called by the WorkQueue to move itself, maybe to a different priority
	 * pool. both from and to can be null. synchronized by the WorkQueue
	 */
	public void moveWorkQueueToPriorityPool(WorkQueue workQueue, Priority from,
			Priority to) {
		if (from == to) {
			return;
		}

		// constant-time removal
		switch (from) {
		case LOW:
			lowWorkQueuePool.remove(workQueue);
			break;
		case MEDIUM:
			mediumWorkQueuePool.remove(workQueue);
			break;
		case HIGH:
			highWorkQueuePool.remove(workQueue);
			break;
		}

		// constant-time addition
		switch (to) {
		case LOW:
			lowWorkQueuePool.add(workQueue);
			break;
		case MEDIUM:
			mediumWorkQueuePool.add(workQueue);
			break;
		case HIGH:
			highWorkQueuePool.add(workQueue);
			break;
		}
	}

	/* only one thread at a time calls this method */
	public void schedule(Priority priority) {

		// we make the component passive or ready
	}

	/* =============== CONFIGURATION =============== */
}
