package se.sics.kompics.core.capability;

import se.sics.kompics.api.Channel;
import se.sics.kompics.core.ComponentCore;

public class ComponentUnsubscribeCapability {

	private ComponentCore componentCore;

	public ComponentUnsubscribeCapability(ComponentCore componentCore) {
		super();
		this.componentCore = componentCore;
	}

	public void unsubscribe(Channel channel, String eventHandlerName) {
		componentCore.unsubscribe(channel, eventHandlerName);
	}
}
