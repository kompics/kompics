package se.sics.kompics.network.grizzly.test;

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

public class PerfServer extends ComponentDefinition {

	public static void main(String[] args) {
		Kompics.createAndStart(PerfServer.class);
	}

	Component grizzly;
	Address self;

	public PerfServer() throws UnknownHostException {
		grizzly = create(GrizzlyNetwork.class);
		subscribe(h, grizzly.provided(Network.class));
		self = new Address(InetAddress.getLocalHost(), 2222, 0);

		trigger(new GrizzlyNetworkInit(self), grizzly.control());
	}

	Handler<TestMessage> h = new Handler<TestMessage>() {
		public void handle(TestMessage event) {
			trigger(new TestMessage(self, event.getSource(), "World".getBytes()),
					grizzly.provided(Network.class));
		}
	};
}
