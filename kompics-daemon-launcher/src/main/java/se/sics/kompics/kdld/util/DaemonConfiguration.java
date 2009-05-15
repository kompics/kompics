package se.sics.kompics.kdld.util;

import java.io.IOException;

import org.apache.commons.cli.Option;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.address.Address;

public class DaemonConfiguration extends Configuration {

	private static final Logger logger = LoggerFactory.getLogger(DaemonConfiguration.class);
	
	public final static String PROP_DAEMON_CONFIG_PROPS_FILE = "config/daemon.properties";
	public final static String PROP_DAEMON_RETRY_PERIOD  = "daemon.retry.period";
	public final static String PROP_DAEMON_RETRY_COUNT  = "daemon.retry.count";

	
	protected int daemonRetryPeriod;
	
	protected int daemonRetryCount;
	
	/********************************************************/
	/********* Helper fields ********************************/
	/********************************************************/
	protected Option daemonRetryPeriodOption;	
	protected Option daemonRetryCountOption;

	protected PropertiesConfiguration daemonConfig;

	/**
	 * 
	 * @param args
	 * @throws IOException
	 */
	public DaemonConfiguration(String[] args) throws IOException, ConfigurationException {
		super(args);

	}

	@Override
	protected void parseAdditionalOptions(String[] args) throws IOException {
		daemonRetryPeriodOption = new Option("drp", true, "Daemon retry period");
		daemonRetryPeriodOption.setArgName("drp");
		options.addOption(daemonRetryPeriodOption);

		daemonRetryCountOption = new Option("drc", true, "Daemon retry count");
		daemonRetryCountOption.setArgName("drc");
		options.addOption(daemonRetryCountOption);

	}

	@Override
	protected void processAdditionalOptions() throws IOException {

		try {
			daemonConfig = new PropertiesConfiguration(PROP_DAEMON_CONFIG_PROPS_FILE);
			daemonConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
			configuration.compositeConfig.addConfiguration(daemonConfig);
		}
		catch (ConfigurationException e)
		{
			logger.warn("Configuration file for cyclon not found, using default values: " + PROP_DAEMON_CONFIG_PROPS_FILE);
		}
		
		if (line.hasOption(daemonRetryPeriodOption.getOpt()))
		{
			int retryPeriod = new Integer(line.getOptionValue(daemonRetryPeriodOption.getOpt()));
			configuration.compositeConfig.setProperty(PROP_DAEMON_RETRY_PERIOD, retryPeriod);
		}
		if (line.hasOption(daemonRetryCountOption.getOpt()))
		{
			int retryCount = new Integer(line.getOptionValue(daemonRetryCountOption.getOpt()));
			configuration.compositeConfig.setProperty(PROP_DAEMON_RETRY_COUNT, retryCount);
		}

	}

	@Override
	protected int getMonitorId() {
		testInitialized();
		return DEFAULT_MONITOR_ID;
	}

	@Override
	protected Address getMonitorServerAddress() {
		testInitialized();
		return new Address(getIp(), getPort(), getMonitorId());
	}
	
	
	public static int getDaemonRetryPeriod() {
		return configuration.compositeConfig.getInt(PROP_DAEMON_RETRY_PERIOD, DEFAULT_RETRY_PERIOD);
	}
	
	public static int getDaemonRetryCount() {
		return configuration.compositeConfig.getInt(PROP_DAEMON_RETRY_COUNT, DEFAULT_RETRY_COUNT);
	}
}
