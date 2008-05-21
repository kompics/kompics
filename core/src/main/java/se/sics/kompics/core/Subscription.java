package se.sics.kompics.core;

public class Subscription {

	private ComponentReference componentReference;

	private ChannelReference channelReference;

	private EventHandler eventHandler;

	public Subscription(ComponentReference componentReference,
			ChannelReference channelReference, EventHandler eventHandler) {
		super();
		this.componentReference = componentReference;
		this.channelReference = channelReference;
		this.eventHandler = eventHandler;
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

	public String toString() {
		return "Subscription for handler " + eventHandler.getName();
	}
}
