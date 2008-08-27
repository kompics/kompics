package se.sics.kompics.network;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.EventHandlerMethod;

@ComponentSpecification
public class EchoClient {

	private final Component component;

	private Channel sendChannel, deliverChannel;

	private int count, messagesInfo[], sum;

	public EchoClient(Component component) {
		super();
		this.component = component;
	}

	@ComponentCreateMethod
	public void create(Channel sendChannel, Channel deliverChannel) {
		this.sendChannel = sendChannel;
		this.deliverChannel = deliverChannel;

		component.subscribe(this.deliverChannel, "handleEchoMessage");
	}

	@ComponentInitializeMethod
	public void init(int count, int sleep, Address client, Address server)
			throws InterruptedException {
		this.count = count;
		messagesInfo = new int[count];
		sum = 0;

		for (int i = 0; i < count; i++) {
			Thread.sleep(sleep);

			EchoMessage echoMessage = new EchoMessage(client, server, i);
			component.triggerEvent(echoMessage, sendChannel);

			messagesInfo[i] = 0;
		}
	}

	@EventHandlerMethod
	public void handleEchoMessage(EchoMessage event) {
		int seq = event.getSequenceNo();

		messagesInfo[seq] = 1;
		sum++;

		if (sum == count) {
			System.out.println("SUM = COUNT = " + count);
			int s = 0;
			for (int i = 0; i < messagesInfo.length; i++) {
				s += messagesInfo[i];
			}
			System.out.println("S = " + s);
		}
	}
}
