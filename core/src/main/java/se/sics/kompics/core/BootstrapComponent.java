package se.sics.kompics.core;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.FaultEvent;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentType;
import se.sics.kompics.api.annotation.EventHandlerMethod;

@ComponentType
public class BootstrapComponent {

	private Component component;

	public BootstrapComponent(Component component) {
		super();
		this.component = component;
	}

	@ComponentCreateMethod
	public void create(Channel channel) {
		component.subscribe(channel, "handleFaultEvent");
	}

	@EventHandlerMethod
	public void handleFaultEvent(FaultEvent event) {
		event.getThrowable().printStackTrace();
	}
}
