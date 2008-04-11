package se.sics.kompics.example;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.FaultEvent;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentType;
import se.sics.kompics.api.annotation.EventHandlerMethod;

@ComponentType
public class FaultHandlerComponent {

	private final Component component;

	public FaultHandlerComponent(Component component) {
		this.component = component;
	}

	@ComponentCreateMethod
	public void create(Channel faultChannel) {
		component.subscribe(faultChannel, "handleFaultEvent");
	}

	@EventHandlerMethod
	public void handleFaultEvent(FaultEvent event) {
		System.out.print("HANDLE FAULT in FAULT HANDLER: ");
		event.getThrowable().printStackTrace(System.out);
	}
}
