package se.sics.kompics.kdld.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Properties;

import org.apache.commons.cli.Option;

import se.sics.kompics.address.Address;

public class CyclonConfiguration extends Configuration {

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

	
	/**
	 * 
	 * @param args
	 * @throws IOException
	 */
	protected CyclonConfiguration(String[] args) throws IOException {
		super(args);
		
		cyclonMonitorServerAddress = new Address(ip, networkPort,
				cyclonMonitorId);

	}

	@Override
	public void parseAdditionalOptions(String[] args) throws IOException {
		
		Properties cyclonProperties = new Properties();
		InputStream cyclonInputStream = Configuration.class
				.getResourceAsStream("cyclon.properties");
		if (cyclonInputStream != null) {

			cyclonProperties.load(cyclonInputStream);

			cyclonMonitorId = Integer.parseInt(cyclonProperties.getProperty("cyclon.monitor.id",
			Integer.toString(DEFAULT_CYCLON_MONITOR_ID)));

			roundTime = Integer.parseInt(cyclonProperties.getProperty("cyclon.roundtime",
					Integer.toString(DEFAULT_ROUND_TIME_MS)));
		}
		
		roundTimeOption = new Option("roundtime", true, "Gossiping round time");
		roundTimeOption.setArgName("milliseconds");
		options.addOption(roundTimeOption);
		
		cyclonMonitorIdOption = new Option("monitorid", true, "Cyclon monitor-id");
		cyclonMonitorIdOption.setArgName("id");
		options.addOption(cyclonMonitorIdOption);

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
