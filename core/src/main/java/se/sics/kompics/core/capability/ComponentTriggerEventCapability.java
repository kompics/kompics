package se.sics.kompics.core.capability;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.Priority;
import se.sics.kompics.core.ComponentCore;

public class ComponentTriggerEventCapability {

	private ComponentCore componentCore;

	public ComponentTriggerEventCapability(ComponentCore componentCore) {
		super();
		this.componentCore = componentCore;
	}

	public void triggerEvent(Event event) {
		componentCore.triggerEvent(event);
	}

	public void triggerEvent(Event event, Channel channel) {
		componentCore.triggerEvent(event, channel);
	}

	public void triggerEvent(Event event, Priority priority) {
		componentCore.triggerEvent(event, priority);
	}

	public void triggerEvent(Event event, Channel channel, Priority priority) {
		componentCore.triggerEvent(event, channel, priority);
	}
}
