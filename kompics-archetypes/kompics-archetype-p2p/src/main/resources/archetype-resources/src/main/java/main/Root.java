package ${package}.main;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;

/**
 * The <code>Root</code> class

 */
public final class Root extends ComponentDefinition {

	private static final Logger logger = LoggerFactory
	.getLogger(Root.class);
	
	private Negative<HelloPort> helloPort = negative(HelloPort.class); 
	private Positive<Network> net = positive(Network.class); 

	private Map<Integer, Address> mapNeighbours = null;
	
	public Root() {
		subscribe(handleStart, control);
		subscribe(handleInit, control);
		subscribe(handleHello, helloPort);
	}

	private Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {
			logger.info("Root started. Waiting for Commands.");
		}
	};  

	private Handler<RootInit> handleInit = new Handler<RootInit>() {
		public void handle(RootInit event) {
			mapNeighbours = event.getNeighbours();
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
			Address dest = mapNeighbours.get(id);
			trigger(new Hello(self, dest), net);
		}
	};  

}
