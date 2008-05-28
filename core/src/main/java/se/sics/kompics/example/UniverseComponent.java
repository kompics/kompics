package se.sics.kompics.example;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Factory;
import se.sics.kompics.api.FaultEvent;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentDestroyMethod;
import se.sics.kompics.api.annotation.ComponentStartMethod;
import se.sics.kompics.api.annotation.ComponentStopMethod;
import se.sics.kompics.api.annotation.ComponentType;
import se.sics.kompics.api.annotation.EventHandlerMethod;

@ComponentType(composite = true)
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

		// create factories for the sub-components
		Factory userFactory = component
				.createFactory("se.sics.kompics.example.UserComponent");
		Factory worldFactory = component
				.createFactory("se.sics.kompics.example.WorldComponent");

		Channel faultChannel = component.createChannel();
		faultChannel.addEventType(FaultEvent.class);
		component.subscribe(faultChannel, "handleFaultEvent");

		// create internal channels
		helloChannel = component.createChannel();
		responseChannel = component.createChannel();

		// add appropriate event types to the internal channels
		helloChannel.addEventType(HelloEvent.class);
		responseChannel.addEventType(ResponseEvent.class);

		// create sub-components
		userComponent = userFactory.createComponent(faultChannel, inputChannel,
				this.outputChannel, helloChannel, responseChannel);
		worldComponent = worldFactory.createComponent(faultChannel,
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
