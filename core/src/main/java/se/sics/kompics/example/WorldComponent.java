package se.sics.kompics.example;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.ComponentStartMethod;
import se.sics.kompics.api.annotation.EventGuardMethod;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.api.annotation.MayTriggerEventTypes;

@ComponentSpecification
public class WorldComponent {

	private final Component component;

	private Channel helloChannel, responseChannel;

	public WorldComponent(Component component) {
		this.component = component;
	}

	@ComponentCreateMethod
	public void create(Channel helloChannel, Channel responseChannel) {
		System.out.println("CREATE WORLD");
		this.helloChannel = helloChannel;
		this.responseChannel = responseChannel;

		component.subscribe(this.helloChannel, "handleHelloEvent");
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
		component.triggerEvent(new ResponseEvent("Hi there!"), responseChannel);
		System.out.println("WORLD TRIGGERED RESPONSE: I replied: Hi there!");
	}

	@EventGuardMethod
	public boolean guardHello(HelloEvent event) {
		System.out.println("TEST GUARD in WORLD");
		return true;
	}
}
