package se.sics.kompics.core;

import se.sics.kompics.api.Event;

public class Binding {

	private ComponentReference component;

	private ChannelReference channel;

	private Class<? extends Event> eventType;

	public Binding(ComponentReference component, ChannelReference channel,
			Class<? extends Event> eventType) {
		super();
		this.component = component;
		this.channel = channel;
		this.eventType = eventType;
	}

	public ComponentReference getComponent() {
		return component;
	}

	public ChannelReference getChannel() {
		return channel;
	}

	public Class<? extends Event> getEventType() {
		return eventType;
	}
}
