package se.sics.kompics.core.capability;

import se.sics.kompics.core.ChannelCore;
import se.sics.kompics.core.EventCore;

public class ChannelPublishCapability {

	private ChannelCore channelCore;

	public ChannelPublishCapability(ChannelCore channelCore) {
		super();
		this.channelCore = channelCore;
	}

	public void publishEventCore(EventCore eventCore) {
		channelCore.publishEventCore(eventCore);
	}
}
