package se.sics.kompics.network;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.EventHandlerMethod;

@ComponentSpecification
public class EchoServer {

	private final Component component;

	private Channel sendChannel, deliverChannel;

	public EchoServer(Component component) {
		super();
		this.component = component;
	}

	@ComponentCreateMethod
	public void create(Channel sendChannel, Channel deliverChannel) {
		this.sendChannel = sendChannel;
		this.deliverChannel = deliverChannel;

		component.subscribe(this.deliverChannel, "handleEchoMessage");
	}

	@EventHandlerMethod
	public void handleEchoMessage(EchoMessage event) {
		int seq = event.getSequenceNo();

		if (seq != EchoTest.echoed) {
			System.err.println("Got " + seq + " expected " + EchoTest.echoed);
			System.exit(0);
		}

		// send echo reply
		EchoMessage message = new EchoMessage(event.getDestination(), event
				.getSource(), seq);
		component.triggerEvent(message, sendChannel);

		EchoTest.echoed++;

		// System.out.println("Echoed seq " + seq);
		// System.out.flush();
	}
}
