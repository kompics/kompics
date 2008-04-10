package se.sics.kompics.core.capability;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Event;
import se.sics.kompics.core.ComponentCore;

public class ComponentBindCapability {

	private ComponentCore componentCore;

	public ComponentBindCapability(ComponentCore componentCore) {
		super();
		this.componentCore = componentCore;
	}

	public void bind(Class<? extends Event> eventType, Channel channel) {
		componentCore.bind(eventType, channel);
	}
}
