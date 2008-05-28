package se.sics.kompics.core;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.capability.ChannelCapabilityFlags;
import se.sics.kompics.core.config.ConfigurationException;
import se.sics.kompics.core.scheduler.Work;

public class ChannelCore {

	private static final Logger logger = LoggerFactory
			.getLogger(ChannelCore.class);

	private HashSet<Class<? extends Event>> eventTypes;

	private HashSet<Class<? extends Event>> eventSubtypes;

	private HashMap<Class<? extends Event>, LinkedList<Subscription>> subscriptions;

	private Object channelLock;

	// TODO fix core visibility
	public ChannelCore(HashSet<Class<? extends Event>> eventTypes) {
		super();
		this.eventTypes = eventTypes;
		eventSubtypes = new HashSet<Class<? extends Event>>();
		subscriptions = new HashMap<Class<? extends Event>, LinkedList<Subscription>>();
		channelLock = new Object();
	}

	/* =============== CONFIGURATION =============== */

	public Set<Class<? extends Event>> getEventTypes() {
		return eventTypes;
	}

	public Set<Class<? extends Event>> getEventSubtypes() {
		return eventSubtypes;
	}

	public LinkedList<Subscription> getSubscriptions(
			Class<? extends Event> eventType) {
		return subscriptions.get(eventType);
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
		if (subscriptions.containsKey(eventType)) {
			throw new ConfigurationException("Cannot remove event type "
					+ eventType.getCanonicalName() + ". Subscription present");
		}
		eventTypes.remove(eventType);
	}

	public boolean hasEventType(Class<? extends Event> eventType) {
		return eventTypes.contains(eventType);
	}

	@SuppressWarnings("unchecked")
	public void addSubscription(Subscription subscription) {
		synchronized (channelLock) {
			Class<? extends Event> eventType = subscription.getEventHandler()
					.getEventType();

			if (!eventTypes.contains(eventType)
					&& !eventSubtypes.contains(eventType)) {
				boolean found = false;
				// check whether the channel has any of the super types
				while (!eventType.equals(Object.class)
						&& Event.class.isAssignableFrom(eventType)) {

					if (eventTypes.contains(eventType)
							|| eventSubtypes.contains(eventType)) {
						found = true;
						logger.debug("ADDING {}", subscription
								.getEventHandler().getEventType());
						eventSubtypes.add(subscription.getEventHandler()
								.getEventType());
						break;
					}
					eventType = (Class<? extends Event>) eventType
							.getSuperclass();
				}

				if (!found) {
					throw new ConfigurationException(
							"Cannot subscribe eventhandler "
									+ subscription.getEventHandler().getName()
									+ " to channel");
				}
			}

			LinkedList<Subscription> subs = subscriptions.get(subscription
					.getEventHandler().getEventType());
			if (subs == null) {
				subs = new LinkedList<Subscription>();
				subs.add(subscription);
				subscriptions.put(
						subscription.getEventHandler().getEventType(), subs);
			} else {
				subs.add(subscription);
			}
		}
	}

	/* =============== EVENT TRIGGERING =============== */
	@SuppressWarnings("unchecked")
	public void publishEventCore(EventCore eventCore) {
		Class<? extends Event> eventType = eventCore.getEvent().getClass();
		Class<? extends Event> eventSupertype = null;

		boolean log = false;
		if (eventCore.getEvent().getClass().getName().endsWith("SignalEvent")) {
			log = true;
			logger.debug("PUB {}", eventCore.getEvent());
		}

		if (!eventTypes.contains(eventType)
				&& !eventSubtypes.contains(eventType)) {
			boolean found = false;
			// check whether the channel has any of the super types
			while (!eventType.equals(Object.class)
					&& Event.class.isAssignableFrom(eventType)) {

				if (log)
					logger.debug("TRYING {}", eventType);

				if (eventTypes.contains(eventType)
						|| eventSubtypes.contains(eventType)) {
					found = true;
					eventSubtypes.add(eventCore.getEvent().getClass());
					eventSupertype = eventType;
					break;
				}
				eventType = (Class<? extends Event>) eventType.getSuperclass();
			}

			if (!found) {
				throw new ConfigurationException("Cannot publish event "
						+ eventType + " in channel");
			}
		}

		if (log) {
			logger.debug("EventType is {}", eventType);
			for (Map.Entry<Class<? extends Event>, LinkedList<Subscription>> entry : subscriptions
					.entrySet()) {
				for (Subscription sub : entry.getValue()) {
					logger.debug("SUB({})={}", entry.getKey(), sub);
				}
			}
			for (Class<? extends Event> eT : eventTypes) {
				logger.debug("ET: {}", eT);
			}
			for (Class<? extends Event> eT : eventSubtypes) {
				logger.debug("EST: {}", eT);
			}
		}

		synchronized (channelLock) {
			eventType = eventCore.getEvent().getClass();
			// add subscriptions to supertype to type
			if (eventSupertype != null) {
				LinkedList<Subscription> superSubs = subscriptions
						.get(eventSupertype);
				LinkedList<Subscription> subs = subscriptions.get(eventType);
				if (subs == null) {
					subs = new LinkedList<Subscription>();
				}
				if (superSubs == null) {
					superSubs = new LinkedList<Subscription>();
				}
				subs.addAll(superSubs);
				subscriptions.put(eventType, subs);
			}

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
