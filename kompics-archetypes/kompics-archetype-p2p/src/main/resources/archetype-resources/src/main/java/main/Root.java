package ${package}.main;

import java.util.List;
import java.util.Set;

import ${package}.main.HelloPort;
import ${package}.main.event.Hello;
import ${package}.main.event.RootInit;
import ${package}.main.event.SendHello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

/**
 * The <code>Root</code> class

 */
public final class Root extends ComponentDefinition {

	private static final Logger logger = LoggerFactory
	.getLogger(Root.class);
	
	private Negative<HelloPort> helloPort = negative(HelloPort.class); 
	private Positive<Network> net = positive(Network.class); 
	private Positive<Timer> timer = positive(Timer.class);

	private List<Address> setNeighbours = null;
	
	private Address self;
	
	public Root() {
		subscribe(handleStart, control);
		subscribe(handleInit, control);
		subscribe(handleHello, net);
		subscribe(handleSendHello, helloPort);
	}

	private Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {
			logger.info("Root started. Waiting for Commands.");
		}
	};  

	private Handler<RootInit> handleInit = new Handler<RootInit>() {
		public void handle(RootInit event) {
			self = event.getSelf();
			setNeighbours = event.getNeighbours();
		}
	};  

	
	private Handler<Hello> handleHello = new Handler<Hello>() {
		public void handle(Hello event) {
			logger.info("Hello Event Received");
		}
	};  

	private Handler<SendHello> handleSendHello = new Handler<SendHello>() {
		public void handle(SendHello event) {
			int id = event.getId();
			Address dest = setNeighbours.get(id);
			if (dest != null)
				trigger(new Hello(self, dest), net);
			else
				System.err.println("Couldn't send hello to neighbour. Couldn't find id: " + id);
		}
	};  

}
