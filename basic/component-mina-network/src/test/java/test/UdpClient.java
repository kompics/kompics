package test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
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

public class UdpClient extends ComponentDefinition {

	public static void main(String[] args) {
		Kompics.createAndStart(UdpClient.class);
	}

	Address c = new Address(InetAddress.getLocalHost(), 3333, 0);
	Address s = new Address(InetAddress.getLocalHost(), 2222, 0);
	Address mcast;

	Component mina;

	public UdpClient() throws UnknownHostException {
		mina = create(MinaNetwork.class);
		subscribe(start, control);
		trigger(new MinaNetworkInit(c), mina.control());

		mcast = new Address(InetAddress.getByName(MulticastReceiver.ADDRESS),
				MulticastReceiver.PORT, 0);
	}

	Handler<Start> start = new Handler<Start>() {
		public void handle(Start event) {

			// for (int i = 1; i <= 10; i++) {
			// byte[] payload = new byte[i * 200000];
			//
			// payload[222] = 8;
			//
			// TestMessage tm = new TestMessage(c, s, payload);
			//
			// trigger(tm, mina.provided(Network.class));
			// }

			String message = "Hello!";

			TestUdpMessage tm = new TestUdpMessage(c, mcast, message.getBytes());
			// TestMessage tm = new TestMessage(c, s, message.getBytes());

			trigger(tm, mina.provided(Network.class));

			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos;
				oos = new ObjectOutputStream(baos);
				oos.writeObject(tm);
				oos.flush();
				oos.close();

				byte[] buf = baos.toByteArray();

				System.err.println("Sent " + tm + " in " + buf.length
						+ " bytes [" + new String(buf) + "]");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};
}
