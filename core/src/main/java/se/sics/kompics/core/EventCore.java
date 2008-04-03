package se.sics.kompics.core;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.Priority;

public class EventCore implements Event {

	private Event event;
	
	private ChannelCore channel;

	private Priority priority;

	public EventCore(Event event, ChannelCore channel, Priority priority) {
		super();
		this.event = event;
		this.channel = channel;
		this.priority = priority;
	}

	public Event getEvent() {
		return event;
	}

	public ChannelCore getChannel() {
		return channel;
	}

	public Priority getPriority() {
		return priority;
	}
}
