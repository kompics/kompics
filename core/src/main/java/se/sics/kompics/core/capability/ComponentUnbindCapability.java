package se.sics.kompics.core.capability;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Event;
import se.sics.kompics.core.ComponentCore;

public class ComponentUnbindCapability {

	private ComponentCore componentCore;

	public ComponentUnbindCapability(ComponentCore componentCore) {
		super();
		this.componentCore = componentCore;
	}

	public void unbind(Class<? extends Event> eventType, Channel channel) {
		componentCore.unbind(eventType, channel);
	}
}
