package se.sics.kompics.wan.main;

import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.mina.MinaNetwork;
import se.sics.kompics.network.mina.MinaNetworkInit;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.config.DaemonConfiguration;
import se.sics.kompics.wan.daemon.Daemon;
import se.sics.kompics.wan.daemon.DaemonInit;
import se.sics.kompics.wan.util.LocalIPAddressNotFound;


public class DaemonMain extends ComponentDefinition {
	

	private Component time;
	private Component network;
	private Component daemon;

	
	private static final Logger logger = LoggerFactory
	.getLogger(DaemonMain.class);

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		
		try {
			Configuration.init(args, DaemonConfiguration.class);
			Kompics.createAndStart(DaemonMain.class,2);			
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * Instantiates a new assignment0 group0.
	 */
	public DaemonMain() {
		
		// create components
		time = create(JavaTimer.class);
		network = create(MinaNetwork.class);
		daemon = create(Daemon.class);		

		// handle possible faults in the components
		subscribe(handleFault, time.getControl());
		subscribe(handleFault, network.getControl());
		subscribe(handleFault, daemon.getControl());

		
		try {
			trigger(new MinaNetworkInit(DaemonConfiguration.getPeer0Address()), network.getControl());
		
			connect(daemon.getNegative(Network.class), network
					.getPositive(Network.class));
			connect(daemon.getNegative(Timer.class), time
					.getPositive(Timer.class));
			
			DaemonInit dInit = new DaemonInit(DaemonConfiguration.getDaemonId(), 
					DaemonConfiguration.getPeer0Address(),
					DaemonConfiguration.getMasterAddress(), 
					DaemonConfiguration.getDaemonRetryPeriod(),
					DaemonConfiguration.getDaemonRetryCount(), 
					DaemonConfiguration.getDaemonIndexingPeriod());
			trigger(dInit, daemon.getControl());

		} catch (LocalIPAddressNotFound e) {
			e.printStackTrace();
			throw new IllegalStateException(e.getMessage());
		}
			
		}

	Handler<Fault> handleFault = new Handler<Fault>() {
		public void handle(Fault fault) {
			fault.getFault().printStackTrace(System.err);
		}
	};


}
