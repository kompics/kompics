package se.sics.kompics.example;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentStartMethod;
import se.sics.kompics.api.annotation.ComponentType;
import se.sics.kompics.api.annotation.EventGuardMethod;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.api.annotation.MayTriggerEventTypes;

@ComponentType
public class WorldComponent {

	private final Component component;

	public WorldComponent(Component component) {
		this.component = component;
	}

	@ComponentStartMethod
	public void start() {
		System.out.println("START WORLD");
	}

	@EventHandlerMethod(guarded = true, guard = "guardHello")
	@MayTriggerEventTypes( { ResponseEvent.class })
	public void handleHelloEvent(HelloEvent event) {
		String message = event.getMessage();
		System.out.println("HANDLE HELLO in WORLD: I got message: \"" + message
				+ "\"");
		component.triggerEvent(new ResponseEvent("Hi there!"));
		System.out.println("WORLD TRIGGERED RESPONSE: I replied: Hi there!");
	}

	@EventGuardMethod
	public boolean guardHello(HelloEvent event) {
		System.out.println("TEST GUARD in WORLD");
		return true;
	}
}
