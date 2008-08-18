package se.sics.kompics.core;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.FaultEvent;
import se.sics.kompics.api.capability.ChannelCapabilityFlags;
import se.sics.kompics.core.config.ConfigurationException;
import se.sics.kompics.core.scheduler.Work;

public class ChannelCore {

	/**
	 * the event types carried by the channel
	 */
	private HashSet<Class<? extends Event>> eventTypes;

	/**
	 * the event types for which subscriptions to this channel exist
	 */
	private HashSet<Class<? extends Event>> subscribedTypes;

	/**
	 * subscriptions registered by event type
	 */
	private HashMap<Class<? extends Event>, LinkedList<Subscription>> subscriptions;

	/**
	 * subscriptions indexed by actual event type and attributes
	 */
	private HashMap<Class<? extends Event>, SubscriptionSet> lookup;

	private Object channelLock;

	// TODO fix core visibility
	public ChannelCore(HashSet<Class<? extends Event>> eventTypes) {
		super();
		this.eventTypes = eventTypes;
		subscribedTypes = new HashSet<Class<? extends Event>>();
		subscriptions = new HashMap<Class<? extends Event>, LinkedList<Subscription>>();
		lookup = new HashMap<Class<? extends Event>, SubscriptionSet>();
		channelLock = new Object();
	}

	/* =============== CONFIGURATION =============== */

	@SuppressWarnings("unchecked")
	public Set<Class<? extends Event>> getEventTypes() {
		return (Set<Class<? extends Event>>) eventTypes.clone();
	}

	public boolean hasEventType(Class<? extends Event> eventType) {
		return eventTypes.contains(eventType);
	}

	public void removeSubscription(Subscription subscription) {
		Class<? extends Event> eventType = subscription.getEventHandler()
				.getEventType();
		synchronized (channelLock) {
			lookup.clear(); // TODO safe removal of sub data
			if (subscribedTypes.contains(eventType)) {
				LinkedList<Subscription> subs = subscriptions.get(eventType);
				boolean removed = subs.remove(subscription);
				if (!removed) {
					throw new ConfigurationException(
							"There was no subscription for handler "
									+ subscription.getEventHandler().getName()
									+ " at this channel");
				}
				if (subs.size() == 0) {
					subscribedTypes.remove(eventType);
				}
			} else {
				throw new ConfigurationException(
						"There is no subscription for " + eventType
								+ " to this channel");
			}
		}
	}

	public void addSubscription(Subscription subscription) {
		Class<? extends Event> eventType = subscription.getEventHandler()
				.getEventType();
		synchronized (channelLock) {
			lookup.clear();
			if (subscribedTypes.contains(eventType)) {
				// there exists already a subscription for this type
				// we just add another one
				subscriptions.get(eventType).add(subscription);
				return;
			}

			if (eventTypes.contains(eventType)) {
				// directly subscribing for an event type associated with the
				// channel
				subscribedTypes.add(eventType);
				LinkedList<Subscription> subs = new LinkedList<Subscription>();
				subs.add(subscription);
				subscriptions.put(eventType, subs);
			} else {
				// check that event type is a sub-type of a carried type
				HashSet<Class<? extends Event>> superTypes = getAllSupertypes(eventType);
				superTypes.retainAll(eventTypes);

				if (superTypes.size() > 0) {
					// subscribing for a sub-type
					subscribedTypes.add(eventType);
					LinkedList<Subscription> subs = new LinkedList<Subscription>();
					subs.add(subscription);
					subscriptions.put(eventType, subs);
				} else {
					throw new ConfigurationException(
							"Cannot subscribe eventhandler "
									+ subscription.getEventHandler().getName()
									+ " to channel. " + eventType
									+ " is not carried by this channel");
				}
			}
		}
	}

	/* =============== EVENT TRIGGERING =============== */
	public void publishEventCore(EventCore eventCore) {
		Event event = eventCore.getEvent();
		Class<? extends Event> eventType = event.getClass();
		boolean typeMatch = false;

		synchronized (channelLock) {
			SubscriptionSet matchingSet = lookup.get(eventType);

			if (matchingSet == null) {
				matchingSet = new SubscriptionSet(eventType);

				for (Class<? extends Event> type : subscribedTypes) {
					// if the published event is a sub-type of this subscribed
					// type
					if (type.isAssignableFrom(eventType)) {
						LinkedList<Subscription> subs = subscriptions.get(type);
						for (Subscription s : subs) {
							matchingSet.addSubscription(s);
							typeMatch = true;
						}
					}
				}
				lookup.put(eventType, matchingSet);
			} else {
				typeMatch = true;
			}

			if (matchingSet.noFilterSubs != null) {
				deliverToSubscribers(eventCore, event, matchingSet.noFilterSubs);
			}
			if (matchingSet.oneFilterSubs != null) {
				deliverToOneFilteredSubscribers(eventCore, event, matchingSet);
			}
			if (matchingSet.manyFilterSubs != null) {
				deliverToManyFilteredSubscribers(eventCore, event,
						matchingSet.manyFilterSubs);
			}
		}
		if (!typeMatch) {
			// check that the event can be published in this channel, i.e., any
			// of its super-types belongs to the channel
			boolean match = false;
			for (Class<? extends Event> type : eventTypes) {
				if (type.isAssignableFrom(eventType))
					match = true;
			}
			if (!match)
				throw new ConfigurationException("Cannot publish event "
						+ eventType + " in channel");
		}
	}

	private void deliverToSubscribers(EventCore eventCore, Event event,
			LinkedList<Subscription> subs) {
		for (Subscription sub : subs) {
			Work work = new Work(this, eventCore, sub.getEventHandler(),
					eventCore.getPriority());
			sub.getComponent().handleWork(work);
		}
	}

	private void deliverToOneFilteredSubscribers(EventCore eventCore,
			Event event, SubscriptionSet set) {

		for (Field f : set.oneFilterSubs.keySet()) {
			LinkedList<Subscription> subs;
			try {
				subs = set.getSubscriptions(f, f.get(event));
				if (subs != null) {
					deliverToSubscribers(eventCore, event, subs);
				}
			} catch (Throwable e) {
				// exception in f.get
				e.printStackTrace(System.err);

				for (LinkedList<Subscription> list : set.oneFilterSubs.get(f)
						.values())
					for (Subscription s : list) {
						// make the subscriber component trigger a fault
						// event since its subscription by event attribute
						// generated an exception
						ComponentReference component = s.getComponent();
						component.triggerEvent(new FaultEvent(e), component
								.getFaultChannel());
					}
			}
		}
	}

	private void deliverToManyFilteredSubscribers(EventCore eventCore,
			Event event, LinkedList<Subscription> subs) {
		for (Subscription sub : subs) {
			boolean match = true;
			try {
				// for each filter
				for (EventAttributeFilterCore filter : sub.getFilters()) {
					if (!filter.checkFilter(event)) {
						match = false;
						break;
					}
				}
			} catch (Throwable e) {
				// make the subscriber component trigger a fault
				// event since its subscription by event attribute
				// generated an exception
				ComponentReference component = sub.getComponent();
				component.triggerEvent(new FaultEvent(e), component
						.getFaultChannel());
			}
			if (match) {
				Work work = new Work(this, eventCore, sub.getEventHandler(),
						eventCore.getPriority());
				sub.getComponent().handleWork(work);
			}
		}
	}

	public ChannelReference createReference() {
		return new ChannelReference(this, EnumSet
				.allOf(ChannelCapabilityFlags.class));
	}

	private static ConcurrentHashMap<Class<? extends Event>, HashSet<Class<? extends Event>>> eventSuperTypes = new ConcurrentHashMap<Class<? extends Event>, HashSet<Class<? extends Event>>>();

	@SuppressWarnings("unchecked")
	private HashSet<Class<? extends Event>> getAllSupertypes(
			Class<? extends Event> eventType) {

		Class<? extends Event> eType = eventType;
		HashSet<Class<? extends Event>> supertypes = eventSuperTypes.get(eType);

		if (supertypes == null) {
			supertypes = new HashSet<Class<? extends Event>>();

			while (!eventType.equals(Object.class)
					&& Event.class.isAssignableFrom(eventType)) {

				supertypes.add(eventType);
				eventType = (Class<? extends Event>) eventType.getSuperclass();
			}
			eventSuperTypes.putIfAbsent(eType, supertypes);
		}

		return (HashSet<Class<? extends Event>>) supertypes.clone();
	}
}
