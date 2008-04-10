package se.sics.kompics.core.capability;

import se.sics.kompics.api.Event;
import se.sics.kompics.core.ChannelCore;

public class ChannelRemoveEventTypeCapability {

	private ChannelCore channelCore;

	public ChannelRemoveEventTypeCapability(ChannelCore channelCore) {
		super();
		this.channelCore = channelCore;
	}

	public void removeEventType(Class<? extends Event> eventType) {
		channelCore.removeEventType(eventType);
	}
}
