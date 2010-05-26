package test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.mina.MinaNetwork;
import se.sics.kompics.network.mina.MinaNetworkInit;

public class Server extends ComponentDefinition {

	public static void main(String[] args) {
		Kompics.createAndStart(Server.class);
	}

	Component mina;

	public Server() throws UnknownHostException {
		mina = create(MinaNetwork.class);
		subscribe(h, mina.provided(Network.class));

		Address self = new Address(InetAddress.getLocalHost(), 2222, 0);

		trigger(new MinaNetworkInit(self), mina.control());
	}

	Handler<TestMessage> h = new Handler<TestMessage>() {
		public void handle(TestMessage event) {
			System.err.println("Received " + event.getPayload().length
					+ " bytes " + event.getPayload()[222]);
		}
	};
}
