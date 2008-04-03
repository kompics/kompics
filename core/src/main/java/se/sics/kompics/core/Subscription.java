package se.sics.kompics.core;

public class Subscription {

	private ComponentCore component;
	
	private ChannelCore channel;
	
	private EventHandler eventHandler;

	public Subscription(ComponentCore component, ChannelCore channel,
			EventHandler eventHandler) {
		super();
		this.component = component;
		this.channel = channel;
		this.eventHandler = eventHandler;
	}

	public ComponentCore getComponent() {
		return component;
	}

	public ChannelCore getChannel() {
		return channel;
	}

	public EventHandler getEventHandler() {
		return eventHandler;
	}
}
