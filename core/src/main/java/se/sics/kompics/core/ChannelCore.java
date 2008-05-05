package se.sics.kompics.core;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.capability.ChannelCapabilityFlags;
import se.sics.kompics.core.config.ConfigurationException;
import se.sics.kompics.core.scheduler.Work;

public class ChannelCore {

	private HashSet<Class<? extends Event>> eventTypes;

	private HashMap<Class<? extends Event>, LinkedList<Subscription>> subscriptions;

	private HashMap<Class<? extends Event>, LinkedList<Binding>> bindings;

	private Object channelLock;

	// TODO fix core visibility
	public ChannelCore() {
		super();
		eventTypes = new HashSet<Class<? extends Event>>();
		subscriptions = new HashMap<Class<? extends Event>, LinkedList<Subscription>>();
		bindings = new HashMap<Class<? extends Event>, LinkedList<Binding>>();
		channelLock = new Object();
	}

	/* =============== CONFIGURATION =============== */

	public Set<Class<? extends Event>> getEventTypes() {
		return eventTypes;
	}

	public LinkedList<Subscription> getSubscriptions(
			Class<? extends Event> eventType) {
		return subscriptions.get(eventType);
	}

	public LinkedList<Binding> getBindings(Class<? extends Event> eventType) {
		return bindings.get(eventType);
	}

	public void addEventType(Class<? extends Event> eventType) {
		if (!eventTypes.contains(eventType)) {
			eventTypes.add(eventType);
		}
	}

	public void removeEventType(Class<? extends Event> eventType) {
		if (!eventTypes.contains(eventType)) {
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

	public boolean hasEventType(Class<? extends Event> eventType) {
		return eventTypes.contains(eventType);
	}

	public void addSubscription(Subscription subscription) {
		synchronized (channelLock) {
			Class<? extends Event> eventType = subscription.getEventHandler()
					.getEventType();

			LinkedList<Subscription> subs = subscriptions.get(eventType);
			if (subs == null) {
				subs = new LinkedList<Subscription>();
				subs.add(subscription);
				subscriptions.put(eventType, subs);
			} else {
				subs.add(subscription);
			}
		}
	}

	public void addBinding(Binding binding) {
		synchronized (channelLock) {
			Class<? extends Event> eventType = binding.getEventType();

			LinkedList<Binding> binds = bindings.get(eventType);
			if (binds == null) {
				binds = new LinkedList<Binding>();
				binds.add(binding);
				bindings.put(eventType, binds);
			} else {
				binds.add(binding);
			}
		}
	}

	/* =============== EVENT TRIGGERING =============== */
	public void publishEventCore(EventCore eventCore) {
		Class<? extends Event> eventType = eventCore.getEvent().getClass();
		if (!eventTypes.contains(eventType)) {
			throw new ConfigurationException("Cannot publish event "
					+ eventType + " in channel");
		}

		synchronized (channelLock) {
			LinkedList<Subscription> subs = subscriptions.get(eventType);
			if (subs == null) {
				return;
			}

			for (Subscription sub : subs) {
				ComponentReference componentReference = sub.getComponent();
				Work work = new Work(this, eventCore, sub.getEventHandler(),
						eventCore.getPriority());
				componentReference.handleWork(work);
			}
		}
	}

	public ChannelReference createReference() {
		return new ChannelReference(this, EnumSet
				.allOf(ChannelCapabilityFlags.class));
	}
}
