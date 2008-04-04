package se.sics.kompics.core.sched;

import se.sics.kompics.core.ChannelCore;
import se.sics.kompics.core.ComponentCore;
import se.sics.kompics.core.EventCore;
import se.sics.kompics.core.EventHandler;

public class Work {

	private ComponentCore componentCore;

	private ChannelCore channelCore;

	private EventCore eventCore;

	private EventHandler eventHandler;

	public Work(ComponentCore componentCore, ChannelCore channelCore,
			EventCore eventCore, EventHandler eventHandler) {
		super();
		this.componentCore = componentCore;
		this.channelCore = channelCore;
		this.eventCore = eventCore;
		this.eventHandler = eventHandler;
	}

	public ComponentCore getComponentCore() {
		return componentCore;
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
}
