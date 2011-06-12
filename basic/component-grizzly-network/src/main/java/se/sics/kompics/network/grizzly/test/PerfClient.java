package se.sics.kompics.network.grizzly.test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.log4j.PropertyConfigurator;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.grizzly.GrizzlyNetwork;
import se.sics.kompics.network.grizzly.GrizzlyNetworkInit;
import se.sics.kompics.network.grizzly.kryo.KryoMessage;

public class PerfClient extends ComponentDefinition {

	static {
		PropertyConfigurator.configureAndWatch("log4j.properties");
	}
	
	public static void main(String[] args) {
		KryoMessage.register(TestMessage.class);

		Kompics.createAndStart(PerfClient.class);
	}

	Address c = new Address(InetAddress.getByName(System
			.getProperty("PERF_CLIENT", "127.0.0.1")), 3333, 0);
	Address s = new Address(InetAddress.getByName(System
			.getProperty("PERF_SERVER", "127.0.0.1")), 2222, 0);

//	Address c = new Address(InetAddress.getLocalHost(), 3333, 0);
//	Address s = new Address(InetAddress.getLocalHost(), 2222, 0);

	Component grizzly;
	long startTime, received = 0, lastTime, lastCount = 0;

	int pipeline = Integer.parseInt(System.getProperty("PIPELINE", "10"));
	
	public PerfClient() throws UnknownHostException {
		System.err.println("Server address is " + s.getIp() + ":" + 2222);
		System.err.println("Client address is " + c.getIp() + ":" + 3333);
		System.err.println("Pipeline is " + pipeline);
		
		grizzly = create(GrizzlyNetwork.class);
		subscribe(start, control);
		subscribe(h, grizzly.provided(Network.class));
		trigger(new GrizzlyNetworkInit(c), grizzly.control());
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

			if (received % 100000 == 0)
				stats();

			trigger(new TestMessage(event.getDestination(), event.getSource(),
					"Hello".getBytes()), grizzly.provided(Network.class));
		}
	};

	void stats() {
		long now = System.nanoTime();
		
		dumpStats("Last 100000 ", now - lastTime, lastCount);
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
