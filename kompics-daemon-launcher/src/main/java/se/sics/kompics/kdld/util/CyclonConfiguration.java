package se.sics.kompics.kdld.util;

import java.io.IOException;

import org.apache.commons.cli.Option;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.address.Address;

public class CyclonConfiguration extends Configuration {

	private static final Logger logger = LoggerFactory.getLogger(CyclonConfiguration.class);
	
	public final static String CYCLON_CONFIG_PROPERTIES_FILE = "config/cyclon.properties";
	
	public final static String PROP_CYCLON_MONITOR_ID = "cyclon.monitorid";
	public final static String PROP_CYCLON_ROUND_TIME = "cyclon.roundtime";
	
	protected final static int DEFAULT_ROUND_TIME_MS = 5000; 
	protected final static int DEFAULT_CYCLON_MONITOR_ID = Integer.MAX_VALUE - 1;

	
	protected Address cyclonMonitorServerAddress = null;

	/********************************************************/
	/********* Helper fields ********************************/
	/********************************************************/
	protected Option roundTimeOption;	
	protected Option cyclonMonitorIdOption;

	protected PropertiesConfiguration cyclonConfig;
	
	/**
	 * 
	 * @param args
	 * @throws IOException
	 */
	public CyclonConfiguration(String[] args) throws IOException, ConfigurationException {
		super(args);

	}

	@Override
	protected void parseAdditionalOptions(String[] args) throws IOException {
		roundTimeOption = new Option("roundtime", true, "Gossiping round time");
		roundTimeOption.setArgName("milliseconds");
		options.addOption(roundTimeOption);
		
		cyclonMonitorIdOption = new Option("monitorid", true, "Cyclon monitor-id");
		cyclonMonitorIdOption.setArgName("id");
		options.addOption(cyclonMonitorIdOption);

	}

	public int getRoundTime() {
		testInitialized();
		return configuration.compositeConfig.getInt(PROP_CYCLON_ROUND_TIME, DEFAULT_ROUND_TIME_MS);
	}

	@Override
	protected void processAdditionalOptions() throws IOException {
		
		try {
			cyclonConfig = new PropertiesConfiguration(CYCLON_CONFIG_PROPERTIES_FILE);
			cyclonConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
			compositeConfig.addConfiguration(cyclonConfig);
		}
		catch (ConfigurationException e)
		{
			logger.warn("Configuration file for cyclon not found, using default values: " + CYCLON_CONFIG_PROPERTIES_FILE);
		}

		if (line.hasOption(roundTimeOption.getOpt()))
		{
			int roundTime = new Integer(line.getOptionValue(roundTimeOption.getOpt()));
			configuration.compositeConfig.setProperty(PROP_CYCLON_ROUND_TIME, roundTime);
		}
		if (line.hasOption(cyclonMonitorIdOption.getOpt()))
		{
			int cyclonMonitorId = new Integer(line.getOptionValue(cyclonMonitorIdOption.getOpt()));
			configuration.compositeConfig.setProperty(PROP_CYCLON_MONITOR_ID, cyclonMonitorId);
		}
	}

	@Override
	protected int getMonitorId() {
		testInitialized();
		return configuration.compositeConfig.getInt(PROP_CYCLON_MONITOR_ID, DEFAULT_CYCLON_MONITOR_ID);
	}

	@Override
	protected Address getMonitorServerAddress() throws LocalIPAddressNotFound {
		testInitialized();
		if (cyclonMonitorServerAddress == null){
			cyclonMonitorServerAddress = new Address(getIp(), getPort(), getMonitorId());
		}			
		return cyclonMonitorServerAddress;
	}
}
