package test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class SimpleMulticastSender {

	public static void main(String[] args) throws IOException,
			InterruptedException, ClassNotFoundException {

		DatagramSocket ds = new DatagramSocket();

		for (int i = 0; i < 3; i++) {
			String message = "Hello multicast! This is a wonderful message. Would you not say that this is a wonderful message?";

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);

			oos.writeObject(message);
			oos.flush();
			oos.close();

			byte[] buffer = baos.toByteArray();

			System.err.println("Message[" + message + "]" + message.length()
					+ " -> ["
					 + new String(buffer)
					+ "]" + buffer.length);

			DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
					InetAddress.getByName(SimpleMulticastReceiver.ADDRESS),
					SimpleMulticastReceiver.PORT);
			ds.send(packet);

			// System.err.println("Sent " + buffer.length + " bytes. Message ["
			// + new String(buffer) + "]");

			Thread.sleep(500);
		}
	}
}
