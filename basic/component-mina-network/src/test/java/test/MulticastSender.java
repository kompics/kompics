package test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.apache.mina.core.buffer.IoBuffer;

public class MulticastSender {

	public static void main(String[] args) throws IOException,
			InterruptedException, ClassNotFoundException {

		DatagramSocket ds = new DatagramSocket(12345);

		Zlib compressor = new Zlib(Zlib.COMPRESSION_MAX, Zlib.MODE_DEFLATER);
		// Zlib decompressor = new Zlib(Zlib.COMPRESSION_MAX,
		// Zlib.MODE_INFLATER);

		for (int i = 0; i < 3; i++) {
			String message = "Hello multicast! This is a wonderful message. Would you not say that this is a wonderful message?";

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);

			oos.writeObject(message);
			oos.flush();
			oos.close();

			IoBuffer uncomp = IoBuffer.wrap(baos.toByteArray());
			IoBuffer comp = compressor.deflate(uncomp);

			System.err.println("U: " + uncomp.capacity() + "/"
					+ uncomp.remaining() + "@" + uncomp.position());
			System.err.println("C: " + comp.capacity() + "/" + comp.remaining()
					+ "@" + comp.position());

			comp.rewind();
			int length = comp.remaining();
			byte[] buffer = new byte[length];
			comp.get(buffer, 0, length);

			System.err.println("Message[" + message + "]" + message.length()
					+ " -> ["
					// + new String(buffer)
					+ "]" + length);

			DatagramPacket packet = new DatagramPacket(buffer, length,
					InetAddress.getByName(MulticastReceiver.ADDRESS),
					MulticastReceiver.PORT);
			ds.send(packet);

			// comp.rewind();
			// System.err.println("C: " + comp.capacity() + "/" +
			// comp.remaining() + "@" + comp.position());
			//
			// IoBuffer test = decompressor.inflate(comp);
			//
			// System.err.println("T: " + test.capacity() + "/" +
			// test.remaining()+ "@" + test.position());
			//
			// int tlength = test.remaining();
			//
			// System.out.println(tlength);
			// byte[] tbuffer = new byte[tlength];
			// test.get(tbuffer, 0 , tlength);
			//
			// ByteArrayInputStream bais = new ByteArrayInputStream(tbuffer);
			// ObjectInputStream ois = new ObjectInputStream(bais);
			//
			// Object o = ois.readObject();
			//
			// System.err.println("Received object [" + o + "]");

			Thread.sleep(500);
		}
	}
}
