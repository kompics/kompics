package se.sics.kompics.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Event;

public class ChannelCore implements Channel {

	private HashSet<Class<? extends Event>> eventTypes;

	private HashMap<Class<? extends Event>, LinkedList<Subscription>> subscriptions;

	private HashMap<Class<? extends Event>, LinkedList<Binding>> bindings;

	public ChannelCore() {
		super();
		eventTypes = new HashSet<Class<? extends Event>>();
		subscriptions = new HashMap<Class<? extends Event>, LinkedList<Subscription>>();
		bindings = new HashMap<Class<? extends Event>, LinkedList<Binding>>();
	}

	public HashSet<Class<? extends Event>> getEventTypes() {
		return eventTypes;
	}

	public LinkedList<Subscription> getSubscriptions(
			Class<? extends Event> eventType) {
		return subscriptions.get(eventType);
	}

	public LinkedList<Binding> getBindings(Class<? extends Event> eventType) {
		return bindings.get(eventType);
	}
}
