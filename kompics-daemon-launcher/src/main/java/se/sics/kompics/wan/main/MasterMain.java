package se.sics.kompics.wan.main;

import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.mina.MinaNetwork;
import se.sics.kompics.network.mina.MinaNetworkInit;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.config.MasterServerConfiguration;
import se.sics.kompics.wan.master.Master;
import se.sics.kompics.wan.master.MasterInit;


public class MasterMain extends ComponentDefinition {
	
	private Component time;
	private Component network;
	private Component master;

	private Address self;
	
	private static final Logger logger = LoggerFactory
	.getLogger(MasterMain.class);
	
	private static MasterServerConfiguration config;

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		
		try {
			config = (MasterServerConfiguration) Configuration.init(args, MasterServerConfiguration.class);
			Kompics.createAndStart(MasterMain.class, 2);			
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * Instantiates a new assignment0 group0.
	 */
	public MasterMain() {
		
		// create components
		time = create(JavaTimer.class);
		network = create(MinaNetwork.class);
		master = create(Master.class);		

		// handle possible faults in the components
		subscribe(handleFault, time.getControl());
		subscribe(handleFault, network.getControl());
		subscribe(handleFault, master.getControl());

		MasterInit init = new 
			MasterInit(config.getBootConfiguration(), config.getMonitorConfiguration());
		
		trigger(init, master.getControl());
		trigger(new MinaNetworkInit(self), network.getControl());
		
		connect(master.getNegative(Network.class), network
				.getPositive(Network.class));
		connect(master.getNegative(Timer.class), time
				.getPositive(Timer.class));
		
	}

	Handler<Fault> handleFault = new Handler<Fault>() {
		public void handle(Fault fault) {
			fault.getFault().printStackTrace(System.err);
		}
	};


}
