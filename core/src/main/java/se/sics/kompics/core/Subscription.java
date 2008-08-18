package se.sics.kompics.core;

public class Subscription {

	private final ComponentReference componentReference;

	private final ChannelReference channelReference;

	private final EventHandler eventHandler;

	private final EventAttributeFilterCore[] filters;

	public Subscription(ComponentReference componentReference,
			ChannelReference channelReference, EventHandler eventHandler,
			EventAttributeFilterCore[] filters) {
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

	public EventAttributeFilterCore[] getFilters() {
		return filters;
	}

	public String toString() {
		return "Subscription for handler " + eventHandler.getName() + " ("
				+ filters.length + ")";
	}
}
