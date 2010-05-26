package test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.mina.MinaNetwork;
import se.sics.kompics.network.mina.MinaNetworkInit;

public class Client extends ComponentDefinition {

	public static void main(String[] args) {
		Kompics.createAndStart(Client.class);
	}

	Address c = new Address(InetAddress.getLocalHost(), 3333, 0);
	Address s = new Address(InetAddress.getLocalHost(), 2222, 0);

	Component mina;

	public Client() throws UnknownHostException {
		mina = create(MinaNetwork.class);
		subscribe(start, control);
		trigger(new MinaNetworkInit(c), mina.control());
	}

	Handler<Start> start = new Handler<Start>() {
		public void handle(Start event) {
			for (int i = 1; i <= 10; i++) {
				byte[] payload = new byte[i * 200000];

				payload[222] = 8;
				
				TestMessage tm = new TestMessage(c, s, payload);
				
				trigger(tm, mina.provided(Network.class));
			}
		}
	};
}
