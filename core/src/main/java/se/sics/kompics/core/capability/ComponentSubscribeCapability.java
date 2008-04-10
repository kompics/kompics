package se.sics.kompics.core.capability;

import se.sics.kompics.api.Channel;
import se.sics.kompics.core.ComponentCore;

public class ComponentSubscribeCapability {

	private ComponentCore componentCore;

	public ComponentSubscribeCapability(ComponentCore componentCore) {
		super();
		this.componentCore = componentCore;
	}

	public void subscribe(Channel channel, String eventHandlerName) {
		componentCore.subscribe(channel, eventHandlerName);
	}
}
