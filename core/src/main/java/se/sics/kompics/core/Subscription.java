package se.sics.kompics.core;


public class Subscription {

	private final ComponentReference componentReference;

	private final ChannelReference channelReference;

	private final EventHandlerCore eventHandlerCore;

	private final EventAttributeFilterCore[] filters;

	public Subscription(ComponentReference componentReference,
			ChannelReference channelReference,
			EventHandlerCore eventHandlerCore,
			EventAttributeFilterCore[] filters) {
		super();
		this.componentReference = componentReference;
		this.channelReference = channelReference;
		this.eventHandlerCore = eventHandlerCore;
		this.filters = filters;
	}

	public ComponentReference getComponent() {
		return componentReference;
	}

	public ChannelReference getChannel() {
		return channelReference;
	}

	public EventHandlerCore getEventHandlerCore() {
		return eventHandlerCore;
	}

	public EventAttributeFilterCore[] getFilters() {
		return filters;
	}
}
