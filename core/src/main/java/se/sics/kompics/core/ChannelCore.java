package se.sics.kompics.core;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Event;
import se.sics.kompics.core.config.ConfigurationException;
import se.sics.kompics.core.sched.Work;

public class ChannelCore implements Channel {

	private HashMap<Class<? extends Event>, ReentrantLock> eventTypes;

	private HashMap<Class<? extends Event>, LinkedList<Subscription>> subscriptions;

	private HashMap<Class<? extends Event>, LinkedList<Binding>> bindings;

	public ChannelCore() {
		super();
		eventTypes = new HashMap<Class<? extends Event>, ReentrantLock>();
		subscriptions = new HashMap<Class<? extends Event>, LinkedList<Subscription>>();
		bindings = new HashMap<Class<? extends Event>, LinkedList<Binding>>();
	}

	/* =============== CONFIGURATION =============== */

	public Set<Class<? extends Event>> getEventTypes() {
		return eventTypes.keySet();
	}

	public LinkedList<Subscription> getSubscriptions(
			Class<? extends Event> eventType) {
		return subscriptions.get(eventType);
	}

	public LinkedList<Binding> getBindings(Class<? extends Event> eventType) {
		return bindings.get(eventType);
	}

	public void addEventType(Class<? extends Event> eventType) {
		if (!eventTypes.containsKey(eventType)) {
			eventTypes.put(eventType, new ReentrantLock());
		}
	}

	public void removeEventType(Class<? extends Event> eventType) {
		if (!eventTypes.containsKey(eventType)) {
			return;
		}
		if (subscriptions.containsKey(eventType)
				|| bindings.containsKey(eventType)) {
			throw new ConfigurationException("Cannot remove event type "
					+ eventType.getCanonicalName()
					+ ". Binding or subscription present");
		}
		eventTypes.remove(eventType);
	}

	/* =============== EVENT TRIGGERING =============== */
	public void publishEventCore(EventCore eventCore) {
		Class<? extends Event> eventType = eventCore.getEvent().getClass();
		if (!eventTypes.containsKey(eventType)) {
			throw new ConfigurationException("Cannot publish event "
					+ eventType + " in channel");
		}

		LinkedList<Subscription> subs = subscriptions.get(eventType);
		if (subs == null)
			return;

		ReentrantLock lock = eventTypes.get(eventType);

		lock.lock();
		for (Subscription sub : subs) {
			ComponentCore componentCore = sub.getComponentCore();
			Work work = new Work(componentCore, this, eventCore, sub
					.getEventHandler());

			componentCore.handleWork(work);
		}
		lock.unlock();
	}
}
