package se.sics.kompics.example;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Factory;
import se.sics.kompics.api.FaultEvent;
import se.sics.kompics.api.Kompics;

public class HelloMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws ClassNotFoundException {

		// get the bootstrap Kompics component
		Kompics kompics = new Kompics(3, 3);
		Component boot = kompics.getBootstrapComponent();

		// create a factory for the Universe component
		Factory universeFactory = boot
				.createFactory("se.sics.kompics.example.UniverseComponent");

		// create a factory for the FaultHandler component
		Factory faultFactory = boot
				.createFactory("se.sics.kompics.example.FaultHandlerComponent");

		// create a fault channel
		Channel faultChannel = boot.createChannel();
		faultChannel.addEventType(FaultEvent.class);

		// create a request and a response channel
		Channel requestChannel = boot.createChannel();
		Channel responseChannel = boot.createChannel();

		// add appropriate event types to the channels
		requestChannel.addEventType(InputEvent.class);
		responseChannel.addEventType(OutputEvent.class);

		// create a universe component
		Component universeComponent = universeFactory.createComponent(
				faultChannel, requestChannel, responseChannel);

		// create a fault handler component
		Component faultHandlerComponent = faultFactory.createComponent(
				faultChannel, faultChannel);

		// start components
		universeComponent.start();
		faultHandlerComponent.start();

		System.out.println("TRIGGER INPUT in MAIN");
		// trigger an input event in the request channel
		boot.triggerEvent(new InputEvent(), requestChannel);
	}
}
