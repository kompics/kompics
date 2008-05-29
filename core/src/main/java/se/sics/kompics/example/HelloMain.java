package se.sics.kompics.example;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
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

		// create a fault channel
		Channel faultChannel = boot.createChannel(FaultEvent.class);

		// create a request and a response channel
		Channel requestChannel = boot.createChannel(InputEvent.class);
		Channel responseChannel = boot.createChannel(OutputEvent.class);

		// create a universe component
		Component universeComponent = boot.createComponent(
				"se.sics.kompics.example.UniverseComponent", faultChannel,
				requestChannel, responseChannel);

		// create a fault handler component
		Component faultHandlerComponent = boot.createComponent(
				"se.sics.kompics.example.FaultHandlerComponent", faultChannel,
				faultChannel);

		// start components
		universeComponent.start();
		faultHandlerComponent.start();

		System.out.println("TRIGGER INPUT in MAIN");
		// trigger an input event in the request channel
		boot.triggerEvent(new InputEvent(1), requestChannel);
		boot.triggerEvent(new InputEvent(2), requestChannel);
	}
}
