package se.sics.kompics.master.main;


import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import passwordstore.ui.Controller;
import passwordstore.ui.PlanetLabGUI;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;
import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.ui.PlanetLabTextUI;


public class Main extends ComponentDefinition {

	private Component plabUI;

	private static final Logger logger = LoggerFactory.getLogger(Main.class);

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

		try {
			Configuration.init(args, PlanetLabConfiguration.class);
			Kompics.createAndStart(Main.class, 2);
		} catch (ConfigurationException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			System.exit(-1);
		}

	}

	/**
	 * Instantiates a new assignment0 group0.
	 */
	public Main() {
		
//		if (PlanetLabConfiguration.isGUI()) {
			// XXX
			plabUI = create(PlanetLabGUI.class);
//		}
//		else {
//			plabUI = create(PlanetLabTextUI.class);
//		}

		subscribe(handleFault, plabUI.getControl());
		
		trigger(new Start(), plabUI.getControl());
		
	}

	Handler<Fault> handleFault = new Handler<Fault>() {
		public void handle(Fault fault) {
			fault.getFault().printStackTrace(System.err);
		}
	};

}
