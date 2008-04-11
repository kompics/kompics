package se.sics.kompics.core;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.Priority;

public class EventCore implements Event {

	private Event event;

	private ChannelReference channelReference;

	private Priority priority;

	public EventCore(Event event, ChannelReference channelReference,
			Priority priority) {
		super();
		this.event = event;
		this.channelReference = channelReference;
		this.priority = priority;
	}

	public Event getEvent() {
		return event;
	}

	public ChannelReference getChannel() {
		return channelReference;
	}

	public Priority getPriority() {
		return priority;
	}
}
