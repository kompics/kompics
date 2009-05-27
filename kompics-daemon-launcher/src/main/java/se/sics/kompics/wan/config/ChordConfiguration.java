package se.sics.kompics.wan.config;

import java.io.IOException;

import org.apache.commons.cli.Option;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.epfd.diamondp.FailureDetectorConfiguration;
import se.sics.kompics.wan.util.LocalIPAddressNotFound;

public class ChordConfiguration extends Configuration {
	private static final Logger logger = LoggerFactory.getLogger(ChordConfiguration.class);
	
	public final static String CHORD_CONFIG_PROPERTIES_FILE = "config/chord.properties";
	
	public final static String PROP_CHORD_MONITOR_ID  = "chord.monitorid";
	public final static String PROP_CHORD_LIVE_PERIOD  = "chord.live.period";
	public final static String PROP_CHORD_SUSPECTED_PERIOD  = "chord.suspected.period";
	public final static String PROP_CHORD_MIN_R_TO  = "chord.min.rto";
	public final static String PROP_CHORD_TIMEOUT_PERIOD_INC  = "chord.timeout.period.inc";
	
	protected final static int DEFAULT_CHORD_MONITOR_ID = Integer.MAX_VALUE - 2;
	protected final static int DEFAULT_LIVE_PERIOD = 1000;
	protected final static int DEFAULT_SUSPECTED_PERIOD = 5000;
	protected final static int DEFAULT_MIN_R_TO =1000;
	protected final static int DEFAULT_TIMEOUT_PERIOD_INC = 0;
	
	protected Address chordMonitorServerAddress = null;
	
	protected FailureDetectorConfiguration fdConfiguration = null;
	
	protected PropertiesConfiguration chordConfig;
	
	/********************************************************/
	/********* Helper fields ********************************/
	/********************************************************/
	protected Option chordMonitorIdOption;
	protected Option livePeriodOption;
	protected Option suspectedPeriodOption;

	protected static boolean chordInitialized = false;
	
	public ChordConfiguration(String[] args) throws IOException, ConfigurationException {
		super(args);
		chordInitialized = true;
	}

	@Override
	protected void parseAdditionalOptions(String[] args) throws IOException {
		chordMonitorIdOption = new Option("monitorid", true, "chord monitor-id");
		chordMonitorIdOption.setArgName(VAL_NUMBER);
		options.addOption(chordMonitorIdOption);
		
		livePeriodOption = new Option("liveperiod", true, "failure detector live-period");
		livePeriodOption.setArgName(VAL_PERIOD_MILLISECS);
		options.addOption(livePeriodOption);
		
		suspectedPeriodOption = new Option("suspectedperiod", true, "failure detector suspected-period");
		suspectedPeriodOption.setArgName(VAL_PERIOD_MILLISECS);
		options.addOption(suspectedPeriodOption);
	}

	@Override
	public void processAdditionalOptions() throws IOException {
		
		try {
			chordConfig = new PropertiesConfiguration(CHORD_CONFIG_PROPERTIES_FILE);
			chordConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
			compositeConfig.addConfiguration(chordConfig);
		}
		catch (ConfigurationException e)
		{
			logger.warn("Configuration file for chord not found, using default values: " + CHORD_CONFIG_PROPERTIES_FILE);
		}

		if (line.hasOption(chordMonitorIdOption.getOpt()))
		{
			int chordMonitorId = new Integer(line.getOptionValue(chordMonitorIdOption.getOpt()));
			configuration.compositeConfig.setProperty(PROP_CHORD_MONITOR_ID, chordMonitorId);
		}
		if (line.hasOption(livePeriodOption.getOpt()))
		{
			int livePeriod = new Integer(line.getOptionValue(livePeriodOption.getOpt()));
			configuration.compositeConfig.setProperty(PROP_CHORD_LIVE_PERIOD, livePeriod);
		}
		if (line.hasOption(suspectedPeriodOption.getOpt()))
		{
			int suspectedPeriod = new Integer(line.getOptionValue(suspectedPeriodOption.getOpt()));
			configuration.compositeConfig.setProperty(PROP_CHORD_SUSPECTED_PERIOD, suspectedPeriod);
		}
	}

	@Override
	protected int getMonitorId() {
		chordInitialized();
		return configuration.compositeConfig.getInt(PROP_CHORD_MONITOR_ID, DEFAULT_CHORD_MONITOR_ID);
	}

	@Override
	protected Address getMonitorServerAddress()  {
		chordInitialized();
		if (chordMonitorServerAddress == null)
		{
			chordMonitorServerAddress = new Address(Configuration.getIp(), Configuration.getPort(),
					getMonitorId());
		}
		return chordMonitorServerAddress;
	}

	public static int getLivePeriod() {
		chordInitialized();
		return configuration.compositeConfig.getInt(PROP_CHORD_LIVE_PERIOD, DEFAULT_LIVE_PERIOD);
	}

	public int getSuspectedPeriod() {
		chordInitialized();
		return configuration.compositeConfig.getInt(PROP_CHORD_SUSPECTED_PERIOD, DEFAULT_SUSPECTED_PERIOD);
	}

	public int getMinRTo() {
		chordInitialized();
		return configuration.compositeConfig.getInt(PROP_CHORD_MIN_R_TO, DEFAULT_MIN_R_TO);
	}

	public int getTimeoutPeriodIncrement() {
		chordInitialized();
		return configuration.compositeConfig.getInt(PROP_CHORD_TIMEOUT_PERIOD_INC, DEFAULT_TIMEOUT_PERIOD_INC);
	}

	public FailureDetectorConfiguration getFdConfiguration() {
		chordInitialized();
		if (fdConfiguration == null)
		{
			fdConfiguration = new FailureDetectorConfiguration(
					getLivePeriod(), getSuspectedPeriod(), getMinRTo(), 
					getTimeoutPeriodIncrement());
		}
		return fdConfiguration;
	}

	static protected void chordInitialized() {
		baseInitialized();
		if (chordInitialized == false)
		{
			throw new IllegalStateException("ChordConfiguration not initialized before use.");
		}
	}
}
