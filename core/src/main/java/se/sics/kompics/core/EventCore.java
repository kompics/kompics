package se.sics.kompics.core;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.Priority;

public class EventCore implements Event {

	private Event event;

	private ChannelCore channelCore;

	private Priority priority;

	public EventCore(Event event, ChannelCore channelCore, Priority priority) {
		super();
		this.event = event;
		this.channelCore = channelCore;
		this.priority = priority;
	}

	public Event getEvent() {
		return event;
	}

	public ChannelCore getChannelCore() {
		return channelCore;
	}

	public Priority getPriority() {
		return priority;
	}
}
