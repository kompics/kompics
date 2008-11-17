package se.sics.kompics.core.stats;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NetStats {

	static final int BUFFER_SIZE = 4096;

	static final int MAX_COUNT = BUFFER_SIZE / 8;

	SocketChannel out;

	ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

	boolean on = true;

	public NetStats() {
		try {
			out = SocketChannel.open();
			out.configureBlocking(true);

			String ss = System.getProperty("statServer");

			if (ss == null) {
				on = false;
				return;
			} else {
				on = true;
			}

			out.connect(new InetSocketAddress(ss, 4444));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int count = 0;

//	private int mCount = 0;
//	private double sum = 0;

	// transmit every event
	public void push(double x) {
		if (!on) {
			return;
		}
		count++;
		buffer.putDouble(x);

		if (count == MAX_COUNT) {
			count = 0;
			// dump to file
			buffer.flip();
			try {
				out.write(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
			buffer.clear();
		}
	}

	// local average before transmit
	// public void push(double x) {
	// if (!on) {
	// return;
	// }
	//
	// count++;
	// sum += x;
	//		
	// if (count == MAX_COUNT) {
	// mCount++;
	//			
	// buffer.putDouble(sum / count);
	//
	// sum = 0;
	// count = 0;
	//
	// if (mCount == MAX_COUNT) {
	// mCount = 0;
	// // dump to file
	// buffer.flip();
	// try {
	// out.write(buffer);
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// buffer.clear();
	// }
	// }
	// }
}
