package test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.network.netty.NettyNetworkInit;

public class PerfClient extends ComponentDefinition {

	public static void main(String[] args) {
		Kompics.createAndStart(PerfClient.class);
	}

	Address c = new Address(InetAddress.getLocalHost(), 3333, 0);
	Address s = new Address(InetAddress.getLocalHost(), 2222, 0);
	Address mcast;

	Component grizzly;
	long startTime, received = 0, lastTime, lastCount = 0;

	int pipeline = 20;
	
	public PerfClient() throws UnknownHostException {
		grizzly = create(NettyNetwork.class, new NettyNetworkInit(c, 3, 0));
		subscribe(start, control);
		subscribe(h, grizzly.provided(Network.class));
	}

	Handler<Start> start = new Handler<Start>() {
		public void handle(Start event) {
			String message = "Hello!";
			
			for (int i = 0; i < pipeline; i++) {
				TestMessage tm = new TestMessage(c, s, message.getBytes());
				trigger(tm, grizzly.provided(Network.class));
			}
			startTime = System.nanoTime();
			lastTime = System.nanoTime();
		}
	};

	Handler<TestMessage> h = new Handler<TestMessage>() {
		public void handle(TestMessage event) {
			received++;
			lastCount++;

			if (received % 10000 == 0)
				stats();

			trigger(new TestMessage(event.getDestination(), event.getSource(),
					"Hello".getBytes()), grizzly.provided(Network.class));
		}
	};

	void stats() {
		long now = System.nanoTime();
		
		dumpStats("Last 10000 ", now - lastTime, lastCount);
		dumpStats("Overall                        ", now - startTime, received);
		
		lastTime = System.nanoTime();
		lastCount = 0;
	}
	
	void dumpStats(String stat, long time, long count) {
		double tput = (double) count;
		double seconds = ((double) time) / 1000000000.0;
		tput /= seconds;
		System.out.println(stat + String.format("%.3f", tput) + " msgs/s");
	}
}
