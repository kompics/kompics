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
	
	protected final static int DEFAULT_ROUND_TIME_MS = 5000; 
	
	protected final static int DEFAULT_CYCLON_MONITOR_ID = Integer.MAX_VALUE - 1;

	
	protected int roundTime;
	
	protected int cyclonMonitorId = DEFAULT_CYCLON_MONITOR_ID;
	
	protected Address cyclonMonitorServerAddress;

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
	protected CyclonConfiguration(String[] args) throws IOException, ConfigurationException {
		super(args);
		
		cyclonMonitorServerAddress = new Address(ip, port,
				cyclonMonitorId);

	}

	@Override
	public void parseAdditionalOptions(String[] args) throws IOException {
		
		try {
			cyclonConfig = new PropertiesConfiguration(CYCLON_CONFIG_PROPERTIES_FILE);
			cyclonMonitorId = cyclonConfig .getInt(PROP_CYCLON_MONITOR_ID);
		}
		catch (ConfigurationException e)
		{
			logger.warn("Configuration file for cyclon not found, using default values: " + CYCLON_CONFIG_PROPERTIES_FILE);
		}

		roundTimeOption = new Option("roundtime", true, "Gossiping round time");
		roundTimeOption.setArgName("milliseconds");
		options.addOption(roundTimeOption);
		
		cyclonMonitorIdOption = new Option("monitorid", true, "Cyclon monitor-id");
		cyclonMonitorIdOption.setArgName("id");
		options.addOption(cyclonMonitorIdOption);

		cyclonMonitorId = cyclonConfig.getInt(PROP_CYCLON_MONITOR_ID);

	}

	public int getRoundTime() {
		return roundTime;
	}

	@Override
	public void processAdditionalOptions() throws IOException {
		if (line.hasOption(roundTimeOption.getOpt()))
		{
			roundTime = new Integer(line.getOptionValue(roundTimeOption.getOpt()));
		}
		if (line.hasOption(cyclonMonitorIdOption.getOpt()))
		{
			cyclonMonitorId = new Integer(line.getOptionValue(cyclonMonitorIdOption.getOpt()));
		}
	}

	@Override
	protected int getMonitorId() {
		return cyclonMonitorId;
	}

	@Override
	protected Address getMonitorServerAddress() {
		return cyclonMonitorServerAddress;
	}
}
