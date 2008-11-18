package se.sics.kompics.core.stats;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

public class NetStatsWorker extends Thread {

	static final int BUFFER_SIZE = 4096;

	static final int COUNT = BUFFER_SIZE / 8;

	NetStatsServer server;

	SocketChannel network;
	
	FileChannel disk;

	ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

	LinkedBlockingQueue<double[]> queue;

	double[] data = new double[COUNT];

	public NetStatsWorker(NetStatsServer server, SocketChannel network,
			FileChannel disk, LinkedBlockingQueue<double[]> queue) {
		this.server = server;
		this.network = network;
		this.queue = queue;
		this.disk = disk;
	}

	public void run() {
		while (true) {
			try {
				do {
					network.read(buffer);
				} while (buffer.remaining() > 0);

				buffer.flip();

				disk.write(buffer);
				
				buffer.rewind();
				
				processBuffer();

				buffer.clear();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	void processBuffer() {
		for (int i = 0; i < COUNT; i++) {
			try {
				data[i] = buffer.getDouble();
			} catch (BufferUnderflowException e) {
				System.err.println("i=" + i);
				System.err.println("3 pos: " + buffer.position() + " lim: "
						+ buffer.limit() + " cap: " + buffer.capacity());
				e.printStackTrace(System.err);
				throw e;
			}
		}
		try {
			queue.put(data);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
