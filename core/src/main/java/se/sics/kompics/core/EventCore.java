package se.sics.kompics.core;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.Priority;

public class EventCore {

	private Event event;

	private ChannelReference channelReference;

	private Priority priority;

	private long triggerTime;
	
	public EventCore(Event event, ChannelReference channelReference,
			Priority priority) {
		this.event = event;
		this.channelReference = channelReference;
		this.priority = priority;
		this.triggerTime = System.nanoTime();
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
	
	public long getQueuingTime() {
		return System.nanoTime() - triggerTime;
	}
}
