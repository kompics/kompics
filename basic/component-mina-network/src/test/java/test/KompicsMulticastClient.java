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

public class KompicsMulticastClient extends ComponentDefinition {

	public static void main(String[] args) {
		Kompics.createAndStart(KompicsMulticastClient.class, 2);
	}

	Address client = new Address(InetAddress.getLocalHost(), 3333, 0);
	InetAddress multicastGroup = InetAddress.getByName("239.240.241.242");
	Address mcsa = new Address(multicastGroup, 3344, 0);

	Component mina;

	public KompicsMulticastClient() throws UnknownHostException {
		mina = create(MinaNetwork.class, new MinaNetworkInit(client));

		subscribe(start, control);
	}

	Handler<Start> start = new Handler<Start>() {
		public void handle(Start event) {
			String message = "Hello!";

			TestMulticastMessage tmm = new TestMulticastMessage(client, mcsa,
					message.getBytes());

			trigger(tmm, mina.provided(Network.class));

			System.out.println("Sent message " + message);
		}
	};
}
