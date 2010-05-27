package test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class SimpleMulticastReceiver {

	public final static String ADDRESS = "239.240.241.242";
	public final static int PORT = 3344;
	public final static int MAX_LENGTH = 65535;

	private static byte[] buffer = new byte[MAX_LENGTH];

	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
		MulticastSocket ms = new MulticastSocket(PORT);
		ms.setReuseAddress(true);
		ms.joinGroup(InetAddress.getByName(ADDRESS));

		while (true) {
			DatagramPacket packet = new DatagramPacket(buffer, MAX_LENGTH);
			System.out.println("Waiting...");
			ms.receive(packet);

			System.out.println("Received " + packet.getLength()
					+ " bytes from " + packet.getAddress() + ":"
					+ packet.getPort() + " (" + packet.getSocketAddress()
					+ ") ");// + new String(packet.getData()));

			ByteArrayInputStream bais = new ByteArrayInputStream(packet
					.getData(), packet.getOffset(), packet.getLength());
			ObjectInputStream ois = new ObjectInputStream(bais);

			Object o = ois.readObject();

			System.err.println("Received object " + o);
		}
	}
}
