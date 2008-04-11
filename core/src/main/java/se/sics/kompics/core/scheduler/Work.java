package se.sics.kompics.core.scheduler;

import se.sics.kompics.api.Priority;
import se.sics.kompics.core.ChannelCore;
import se.sics.kompics.core.EventCore;
import se.sics.kompics.core.EventHandler;

public class Work {

	private ChannelCore channelCore;

	private EventCore eventCore;

	private EventHandler eventHandler;

	private Priority priority;

	public Work(ChannelCore channelCore, EventCore eventCore,
			EventHandler eventHandler, Priority priority) {
		super();
		this.channelCore = channelCore;
		this.eventCore = eventCore;
		this.eventHandler = eventHandler;
		this.priority = priority;
	}

	public ChannelCore getChannelCore() {
		return channelCore;
	}

	public EventCore getEventCore() {
		return eventCore;
	}

	public EventHandler getEventHandler() {
		return eventHandler;
	}

	public Priority getPriority() {
		return priority;
	}
}
