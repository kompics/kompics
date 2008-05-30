package se.sics.kompics.core;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.FaultEvent;
import se.sics.kompics.api.capability.ChannelCapabilityFlags;
import se.sics.kompics.core.config.ConfigurationException;
import se.sics.kompics.core.scheduler.Work;

public class ChannelCore {

	// private static final Logger logger = LoggerFactory
	// .getLogger(ChannelCore.class);

	/**
	 * the event types carried by the channel
	 */
	private HashSet<Class<? extends Event>> eventTypes;

	/**
	 * the event types for which subscriptions to this channel exist
	 */
	private HashSet<Class<? extends Event>> subscribedTypes;

	/**
	 * the event superTypes of the subscribed event types
	 */
	private HashSet<Class<? extends Event>> subscribedSuperTypes;

	/**
	 * subscriptions registered by event type
	 */
	private HashMap<Class<? extends Event>, LinkedList<Subscription>> subscriptions;

	private Object channelLock;

	// TODO fix core visibility
	public ChannelCore(HashSet<Class<? extends Event>> eventTypes) {
		super();
		this.eventTypes = eventTypes;
		subscribedTypes = new HashSet<Class<? extends Event>>();
		subscribedSuperTypes = new HashSet<Class<? extends Event>>();
		subscriptions = new HashMap<Class<? extends Event>, LinkedList<Subscription>>();
		channelLock = new Object();
	}

	/* =============== CONFIGURATION =============== */

	@SuppressWarnings("unchecked")
	public Set<Class<? extends Event>> getEventTypes() {
		synchronized (channelLock) {
			return (Set<Class<? extends Event>>) eventTypes.clone();
		}
	}

	public void addEventType(Class<? extends Event> eventType) {
		synchronized (channelLock) {
			if (!eventTypes.contains(eventType)) {
				eventTypes.add(eventType);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void removeEventType(Class<? extends Event> eventType) {
		synchronized (channelLock) {
			if (!eventTypes.contains(eventType)) {
				return;
			}

			HashSet<Class<? extends Event>> localSubscribedSuperTypes = (HashSet<Class<? extends Event>>) subscribedSuperTypes
					.clone();

			// check that the event type to be removed is not a super type of a
			// type for which a subscription exists
			if (localSubscribedSuperTypes.contains(eventType)) {
				throw new ConfigurationException("Cannot remove event type "
						+ eventType.getCanonicalName()
						+ ". Subscription present");
			}

			eventTypes.remove(eventType);
		}
	}

	public boolean hasEventType(Class<? extends Event> eventType) {
		synchronized (channelLock) {
			return eventTypes.contains(eventType);
		}
	}

	// TODO removeSubscription

	public void addSubscription(Subscription subscription) {
		synchronized (channelLock) {
			Class<? extends Event> eventType = subscription.getEventHandler()
					.getEventType();

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

					// we keep all the super-types of the subscribed type so
					// that we can prevent the removal of one of them from the
					// channel
					subscribedSuperTypes
							.addAll(getAllSupertypesTillLocalType(eventType));

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
		Class<? extends Event> eventType = eventCore.getEvent().getClass();

		HashSet<Class<? extends Event>> eventSuperTypes = getAllSupertypes(eventType);

		synchronized (channelLock) {
			// check that the event can be published in this channel, i.e., any
			// of its super-types belongs to the channel
			eventSuperTypes.retainAll(eventTypes);

			if (eventSuperTypes.size() == 0) {
				throw new ConfigurationException("Cannot publish event "
						+ eventType + " in channel");
			}

			// we get all super-types of the event for which subscriptions exist
			eventSuperTypes = getAllSupertypes(eventType);
			eventSuperTypes.retainAll(subscribedTypes);

			for (Class<? extends Event> eType : eventSuperTypes) {
				LinkedList<Subscription> subs = subscriptions.get(eType);

				for (Subscription sub : subs) {
					boolean match = true;

					ComponentReference component = sub.getComponent();

					try {
						// for each filter
						for (EventAttributeFilterCore filter : sub.getFilters()) {
							if (!filter.checkFilter(eventCore.getEvent())) {
								match = false;
								break;
							}
						}
					} catch (Exception e) {
						// make the subscriber component trigger a fault event
						// since its subscription by event attribute generated
						// an exception
						component.triggerEvent(new FaultEvent(e), component
								.getFaultChannel());
					}

					if (match) {
						Work work = new Work(this, eventCore, sub
								.getEventHandler(), eventCore.getPriority());
						component.handleWork(work);
					}
				}
			}
		}
	}

	public ChannelReference createReference() {
		return new ChannelReference(this, EnumSet
				.allOf(ChannelCapabilityFlags.class));
	}

	@SuppressWarnings("unchecked")
	private HashSet<Class<? extends Event>> getAllSupertypes(
			Class<? extends Event> eventType) {

		HashSet<Class<? extends Event>> supertypes = new HashSet<Class<? extends Event>>();

		while (!eventType.equals(Object.class)
				&& Event.class.isAssignableFrom(eventType)) {

			supertypes.add(eventType);
			eventType = (Class<? extends Event>) eventType.getSuperclass();
		}
		return supertypes;
	}

	@SuppressWarnings("unchecked")
	private HashSet<Class<? extends Event>> getAllSupertypesTillLocalType(
			Class<? extends Event> eventType) {

		HashSet<Class<? extends Event>> supertypes = new HashSet<Class<? extends Event>>();

		while (!eventType.equals(Object.class)
				&& eventTypes.contains(eventType)
				&& Event.class.isAssignableFrom(eventType)) {

			supertypes.add(eventType);
			eventType = (Class<? extends Event>) eventType.getSuperclass();
		}
		return supertypes;
	}
}
