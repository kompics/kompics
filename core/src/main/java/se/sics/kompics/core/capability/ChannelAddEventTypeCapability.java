package se.sics.kompics.core.capability;

import se.sics.kompics.api.Event;
import se.sics.kompics.core.ChannelCore;

public class ChannelAddEventTypeCapability {

	private ChannelCore channelCore;

	public ChannelAddEventTypeCapability(ChannelCore channelCore) {
		super();
		this.channelCore = channelCore;
	}

	public void addEventType(Class<? extends Event> eventType) {
		channelCore.addEventType(eventType);
	}
}
