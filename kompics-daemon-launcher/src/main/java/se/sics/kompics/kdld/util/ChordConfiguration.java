package se.sics.kompics.kdld.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.cli.Option;

import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.epfd.diamondp.FailureDetectorConfiguration;

public class ChordConfiguration extends Configuration {

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
	
	
	/********************************************************/
	/********* Helper fields ********************************/
	/********************************************************/
	protected Option chordMonitorIdOption;
	protected Option livePeriodOption;
	protected Option suspectedPeriodOption;

	
	public ChordConfiguration(String[] args) throws IOException {
		super(args);

		chordMonitorServerAddress = new Address(ip, networkPort,
				chordMonitorId);
		
		fdConfiguration = new FailureDetectorConfiguration(
				livePeriod, suspectedPeriod, minRTo, 
				timeoutPeriodIncrement);
	}

	@Override
	public void parseAdditionalOptions(String[] args) throws IOException {
		
		Properties chordProperties = new Properties();
		InputStream chordInputStream = Configuration.class
				.getResourceAsStream("chord.properties");
		if (chordInputStream != null) {

			chordProperties.load(chordInputStream);

			chordMonitorId = Integer.parseInt(chordProperties.getProperty("chord.monitorid",
					Integer.toString(DEFAULT_CHORD_MONITOR_ID)));
			livePeriod = Integer.parseInt(chordProperties.getProperty("chord.live.period",
					Integer.toString(DEFAULT_LIVE_PERIOD)));
			suspectedPeriod = Integer.parseInt(chordProperties.getProperty("chord.suspected.period",
					Integer.toString(DEFAULT_SUSPECTED_PERIOD)));
			minRTo = Integer.parseInt(chordProperties.getProperty("chord.min.rto",
					Integer.toString(DEFAULT_MIN_R_TO)));
			timeoutPeriodIncrement = Integer.parseInt(chordProperties.getProperty("chord.timeout.period.inc",
					Integer.toString(DEFAULT_TIMEOUT_PERIOD_INC)));
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
