package se.sics.kompics.core;

import se.sics.kompics.api.Event;

public class Binding {

	private ComponentCore component;
	
	private ChannelCore channel;
	
	private Class<? extends Event> eventType;

	public Binding(ComponentCore component, ChannelCore channel,
			Class<? extends Event> eventType) {
		super();
		this.component = component;
		this.channel = channel;
		this.eventType = eventType;
	}

	public ComponentCore getComponent() {
		return component;
	}

	public ChannelCore getChannel() {
		return channel;
	}

	public Class<? extends Event> getEventType() {
		return eventType;
	}
}
