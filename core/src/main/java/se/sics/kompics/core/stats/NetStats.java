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

			String ss = System.getProperty("statServer", "127.0.0.1");
			
			if (ss.equals("off")) {
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

	public int getCount() {
		return count;
	}

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
}
