package se.sics.kompics.kdld.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.cli.Option;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.epfd.diamondp.FailureDetectorConfiguration;

public class ChordConfiguration extends Configuration {
	private static final Logger logger = LoggerFactory.getLogger(ChordConfiguration.class);
	
	public final static String CHORD_CONFIG_PROPERTIES_FILE = "config/chord.properties";
	
	public final static String PROP_CHORD_MONITOR_ID  = "chord.monitorid";
	public final static String PROP_LIVE_PERIOD  = "chord.live.period";
	public final static String PROP_SUSPECTED_PERIOD  = "chord.suspected.period";
	public final static String PROP_MIN_R_TO  = "chord.min.rto";
	public final static String PROP_TIMEOUT_PERIOD_INC  = "chord.timeout.period.inc";
	
	protected final static int DEFAULT_CHORD_MONITOR_ID = Integer.MAX_VALUE - 2;
	protected final static int DEFAULT_LIVE_PERIOD = 1000;
	protected final static int DEFAULT_SUSPECTED_PERIOD = 5000;
	protected final static int DEFAULT_MIN_R_TO =1000;
	protected final static int DEFAULT_TIMEOUT_PERIOD_INC = 0;
	
	protected int chordMonitorId = DEFAULT_CHORD_MONITOR_ID;
	
	protected Address chordMonitorServerAddress;
	
	protected int livePeriod = DEFAULT_LIVE_PERIOD;
	
	protected int suspectedPeriod = DEFAULT_SUSPECTED_PERIOD;
	
	protected int minRTo = DEFAULT_MIN_R_TO;
	
	protected int timeoutPeriodIncrement = DEFAULT_TIMEOUT_PERIOD_INC;

	protected FailureDetectorConfiguration fdConfiguration;
	
	protected PropertiesConfiguration chordConfig;
	
	/********************************************************/
	/********* Helper fields ********************************/
	/********************************************************/
	protected Option chordMonitorIdOption;
	protected Option livePeriodOption;
	protected Option suspectedPeriodOption;

	
	public ChordConfiguration(String[] args) throws IOException, ConfigurationException {
		super(args);
		
		chordMonitorServerAddress = new Address(ip, port,
				chordMonitorId);
		
		fdConfiguration = new FailureDetectorConfiguration(
				livePeriod, suspectedPeriod, minRTo, 
				timeoutPeriodIncrement);
	}

	@Override
	public void parseAdditionalOptions(String[] args) throws IOException {
		
		try {
			chordConfig = new PropertiesConfiguration(CHORD_CONFIG_PROPERTIES_FILE);
			chordMonitorId = chordConfig.getInt(PROP_CHORD_MONITOR_ID);
			livePeriod = chordConfig.getInt(PROP_LIVE_PERIOD);
			suspectedPeriod = chordConfig.getInt(PROP_SUSPECTED_PERIOD);
			minRTo = chordConfig.getInt(PROP_MIN_R_TO);
			timeoutPeriodIncrement = chordConfig.getInt(PROP_TIMEOUT_PERIOD_INC);
		}
		catch (ConfigurationException e)
		{
			logger.warn("Configuration file for chord not found, using default values: " + CHORD_CONFIG_PROPERTIES_FILE);
		}
		
		chordMonitorIdOption = new Option("monitorid", true, "chord monitor-id");
		chordMonitorIdOption.setArgName("id");
		options.addOption(chordMonitorIdOption);
		
		livePeriodOption = new Option("liveperiod", true, "failure detector live-period");
		livePeriodOption.setArgName("liveperiod");
		options.addOption(livePeriodOption);
		
		suspectedPeriodOption = new Option("suspectedperiod", true, "failure detector suspected-period");
		suspectedPeriodOption.setArgName("suspectedperiod");
		options.addOption(suspectedPeriodOption);

	}

	@Override
	public void processAdditionalOptions() throws IOException {
		if (line.hasOption(chordMonitorIdOption.getOpt()))
		{
			chordMonitorId = new Integer(line.getOptionValue(chordMonitorIdOption.getOpt()));
		}
		if (line.hasOption(livePeriodOption.getOpt()))
		{
			livePeriod = new Integer(line.getOptionValue(livePeriodOption.getOpt()));
		}
		if (line.hasOption(suspectedPeriodOption.getOpt()))
		{
			suspectedPeriod = new Integer(line.getOptionValue(suspectedPeriodOption.getOpt()));
		}
	}

	@Override
	protected int getMonitorId() {
		return chordMonitorId;
	}

	@Override
	protected Address getMonitorServerAddress() {
		return chordMonitorServerAddress;
	}

}
