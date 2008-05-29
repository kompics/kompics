package se.sics.kompics.core;

import se.sics.kompics.api.EventAttributeFilter;

public class Subscription {

	private final ComponentReference componentReference;

	private final ChannelReference channelReference;

	private final EventHandler eventHandler;

	private final EventAttributeFilter[] filters;

	public Subscription(ComponentReference componentReference,
			ChannelReference channelReference, EventHandler eventHandler,
			EventAttributeFilter[] filters) {
		super();
		this.componentReference = componentReference;
		this.channelReference = channelReference;
		this.eventHandler = eventHandler;
		this.filters = filters;
	}

	public ComponentReference getComponent() {
		return componentReference;
	}

	public ChannelReference getChannel() {
		return channelReference;
	}

	public EventHandler getEventHandler() {
		return eventHandler;
	}

	public EventAttributeFilter[] getFilters() {
		return filters;
	}

	public String toString() {
		return "Subscription for handler " + eventHandler.getName();
	}
}
