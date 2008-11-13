package se.sics.kompics.core.stats;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

public class NetStatsWorker extends Thread {

	static final int BUFFER_SIZE = 4096;

	static final int COUNT = BUFFER_SIZE / 8;
	
		NetStatsServer server;

	SocketChannel channel;

	ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

	LinkedBlockingQueue<double[]> queue;
	
	double[] data = new double[COUNT];
	
	public NetStatsWorker(NetStatsServer server, SocketChannel channel, LinkedBlockingQueue<double[]> queue) {
		this.server = server;
		this.channel = channel;
		this.queue = queue;
	}

	public void run() {
		while (true) {
			try {
				channel.read(buffer);
				buffer.flip();
				
				processBuffer();
				
				buffer.clear();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	
	void processBuffer() {
		for (int i = 0; i < COUNT; i++) {
			data[i] = buffer.getDouble();
		}		
		try {
			queue.put(data);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
