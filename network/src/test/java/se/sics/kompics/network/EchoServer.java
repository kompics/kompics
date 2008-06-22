package se.sics.kompics.network;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.network.events.NetworkSendEvent;

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
		NetworkSendEvent sendEvent = new NetworkSendEvent(message, event
				.getDestination(), event.getSource(), Transport.TCP);
		component.triggerEvent(sendEvent, sendChannel);

		EchoTest.echoed++;

		// System.out.println("Echoed seq " + seq);
		// System.out.flush();
	}
}
