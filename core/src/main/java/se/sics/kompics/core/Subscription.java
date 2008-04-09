package se.sics.kompics.core;


public class Subscription {

	private ComponentCore componentCore;

	private ChannelCore channelCore;

	private EventHandler eventHandler;

	public Subscription(ComponentCore componentCore, ChannelCore channelCore,
			EventHandler eventHandler) {
		super();
		this.componentCore = componentCore;
		this.channelCore = channelCore;
		this.eventHandler = eventHandler;
	}

	public ComponentCore getComponentCore() {
		return componentCore;
	}

	public ChannelCore getChannelCore() {
		return channelCore;
	}

	public EventHandler getEventHandler() {
		return eventHandler;
	}
}
