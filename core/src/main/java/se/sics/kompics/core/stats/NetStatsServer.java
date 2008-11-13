package se.sics.kompics.core.stats;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

public class NetStatsServer {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		NetStatsServer server = new NetStatsServer();
		server.bind();

		queue = new LinkedBlockingQueue<double[]>();
		NetStatsProcessor processor = new NetStatsProcessor(queue);
		processor.start();
		
		server.serve();
	}

	ServerSocketChannel serverChannel;
	
	static LinkedBlockingQueue<double[]> queue;
	
	public void bind() throws IOException {
		serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(true);
		serverChannel.socket().bind(new InetSocketAddress(4444));
		System.out.println("NetStatsServer listening on " + serverChannel.socket().getLocalSocketAddress());
	}

	public void serve() throws IOException {
		while(true) {
			SocketChannel channel = serverChannel.accept();
			channel.configureBlocking(true);
			new NetStatsWorker(this, channel, queue).start();
		}
	}
	
	
}
