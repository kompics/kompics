package se.sics.kompics.example;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.FaultEvent;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentDestroyMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.ComponentStartMethod;
import se.sics.kompics.api.annotation.ComponentStopMethod;
import se.sics.kompics.api.annotation.EventHandlerMethod;

@ComponentSpecification(composite = true)
public class UniverseComponent {

	private final Component component;

	// formal parameter channels
	private Channel inputChannel;
	private Channel outputChannel;

	// internal channels
	private Channel helloChannel;
	private Channel responseChannel;

	// internal components
	private Component userComponent;
	private Component worldComponent;

	public UniverseComponent(Component component) {
		this.component = component;
	}

	@ComponentCreateMethod
	public void create(Channel inputChannel, Channel outputChannel)
			throws ClassNotFoundException {
		System.out.println("CREATE UNIVERSE");

		// channel parameters type check
		if (!inputChannel.hasEventType(InputEvent.class)) {
			throw new RuntimeException(
					"inputChannel does not support InputEvent");
		}
		if (!outputChannel.hasEventType(OutputEvent.class)) {
			throw new RuntimeException(
					"outputChannel does not support OutputEvent");
		}

		// accept the actual channels for formal parameters
		this.inputChannel = inputChannel;
		this.outputChannel = outputChannel;

		// subscribe this component to the input channel
		component.subscribe(this.inputChannel, "handleInputEvent");

		Channel faultChannel = component.createChannel(FaultEvent.class);

		component.subscribe(faultChannel, "handleFaultEvent");

		// create internal channels
		helloChannel = component.createChannel(HelloEvent.class);
		responseChannel = component.createChannel(ResponseEvent.class);

		// create sub-components
		userComponent = component
				.createComponent("se.sics.kompics.example.UserComponent",
						faultChannel, inputChannel, this.outputChannel,
						helloChannel, responseChannel);
		userComponent.initialize();

		worldComponent = component.createComponent(
				"se.sics.kompics.example.WorldComponent", faultChannel,
				helloChannel, responseChannel);
	}

	@ComponentDestroyMethod
	public void destroy() {
		System.out.println("DESTROY UNIVERSE");
	}

	@ComponentStartMethod
	public void start() {
		System.out.println("START UNIVERSE");
		userComponent.start();
		worldComponent.start();
	}

	@ComponentStopMethod
	public void stop() {
		System.out.println("STOP UNIVERSE");
		userComponent.stop();
		worldComponent.stop();
	}

	@EventHandlerMethod
	public void handleInputEvent(InputEvent event) {
		System.out.println("HANDLE INPUT in UNIVERSE");
	}

	@EventHandlerMethod
	public void handleFaultEvent(FaultEvent event) throws Throwable {
		System.out.println("HANDLE FAULT in UNIVERSE");
		throw event.getThrowable();
	}
}
