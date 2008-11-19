package se.sics.kompics.core.stats;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

public class NetStatsServer {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		final NetStatsServer server = new NetStatsServer();
		server.bind();

		queue = new LinkedBlockingQueue<double[]>();
		NetStatsProcessor processor = new NetStatsProcessor(queue);
		processor.start();

		new Thread() {
			@Override
			public void run() {
				try {
					server.serve();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			String line = in.readLine();
			if (line == null)
				break;

			if (line.startsWith("measure")) {
				line = line.substring(7);
				int cnt = Integer.parseInt(line);
				server.measure(cnt);
			} else {
				System.out
						.println("Ignored. Type measure4000 if you just joine 4000.");
			}
		}
	}

	private int count = 0;

	private static final int MEASURING_TIME = Integer.parseInt(System
			.getProperty("measuringTime", "180000"));

	private void measure(int cnt) {
		count += cnt;

		System.out.println("I'm starting to measure. Stop typing!");

		long time = System.currentTimeMillis();

		try {
			Thread.sleep(MEASURING_TIME);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		time = System.currentTimeMillis() - time;

		System.out.println("I've been measuring for " + time / 1000
				+ " seconds. You can join more peers now!");
	}

	ServerSocketChannel serverChannel;

	static LinkedBlockingQueue<double[]> queue;

	NetStatsWorker workers[] = new NetStatsWorker[10];

	int workerCount = 0;

	private FileChannel bin;

	public NetStatsServer() throws FileNotFoundException {
		bin = new FileOutputStream("data.bin", false).getChannel();
	}

	public void bind() throws IOException {
		serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(true);
		serverChannel.socket().bind(new InetSocketAddress(4444));
		System.out.println("NetStatsServer listening on "
				+ serverChannel.socket().getLocalSocketAddress());
	}

	public void serve() throws IOException {
		while (true) {
			SocketChannel channel = serverChannel.accept();
			channel.configureBlocking(true);
			workers[workerCount] = new NetStatsWorker(this, channel, bin, queue);
			workers[workerCount].start();

			synchronized (workers) {
				workerCount++;
			}

			System.out.println("Worker " + workerCount + " started");
		}
	}
}
