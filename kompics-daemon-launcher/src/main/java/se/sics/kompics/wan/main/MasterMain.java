package se.sics.kompics.wan.main;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
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
import se.sics.kompics.wan.config.MasterConfiguration;
import se.sics.kompics.wan.master.Master;
import se.sics.kompics.wan.master.MasterInit;

public class MasterMain extends ComponentDefinition {

	private Component time;
	private Component network;
	private Component master;

	private static final Logger logger = LoggerFactory.getLogger(MasterMain.class);

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
		System.out.println("Configuring log4j...");
		rootLogger.setLevel(Level.INFO);
		rootLogger.addAppender(new ConsoleAppender(new PatternLayout("%m%n")));
		
		// root.addAppender(new ConsoleAppender(
		// new PatternLayout(PatternLayout.DEFAULT_CONVERSION_PATTERN)));

		// Logger pkgLogger =
		// rootLogger.getLoggerRepository().getLogger("robertmaldon.moneymachine");
		// pkgLogger.setLevel(Level.DEBUG);
		// pkgLogger.addAppender(new ConsoleAppender(
		// new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN)));

		try {
			Configuration.init(args, MasterConfiguration.class);
			Kompics.createAndStart(MasterMain.class, 2);
		} catch (ConfigurationException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			System.exit(-1);
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

		MasterInit init = new MasterInit(MasterConfiguration.getMasterAddress(),
				MasterConfiguration.getBootConfiguration(), MasterConfiguration
						.getMonitorConfiguration());

		trigger(init, master.getControl());
		trigger(new MinaNetworkInit(MasterConfiguration.getMasterAddress()), network.getControl());

		logger.info("Master listening on: {}", MasterConfiguration.getMasterAddress().toString());

		connect(master.getNegative(Network.class), network.getPositive(Network.class));
		connect(master.getNegative(Timer.class), time.getPositive(Timer.class));

	}

	Handler<Fault> handleFault = new Handler<Fault>() {
		public void handle(Fault fault) {
			fault.getFault().printStackTrace(System.err);
		}
	};

}
