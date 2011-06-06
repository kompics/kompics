package test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.grizzly.GrizzlyNetwork;
import se.sics.kompics.network.grizzly.GrizzlyNetworkInit;

public class TestServer extends ComponentDefinition {

	public static void main(String[] args) {
		Kompics.createAndStart(TestServer.class);
	}

	Component netty;
	Address self;

	public TestServer() throws UnknownHostException {
		netty = create(GrizzlyNetwork.class);
		subscribe(h, netty.provided(Network.class));
		self = new Address(InetAddress.getLocalHost(), 2222, 0);

		trigger(new GrizzlyNetworkInit(self), netty.control());
	}

	Handler<TestMessage> h = new Handler<TestMessage>() {
		public void handle(TestMessage event) {
			System.err.println("Received " + event.getPayload().length
					+ " bytes " + new String(event.getPayload()));

			trigger(new TestMessage(self, event.getSource(), "World".getBytes()),
					netty.provided(Network.class));
		}
	};
}
