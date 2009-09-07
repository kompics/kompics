package se.sics.kompics.wan.config;

import java.io.IOException;

import org.apache.commons.cli.Option;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.address.Address;
import se.sics.kompics.wan.util.AddressParser;
import se.sics.kompics.wan.util.AddressParserException;
import se.sics.kompics.wan.util.LocalNetworkConfiguration;

public abstract class MasterAddressConfiguration extends Configuration {

	private static final Logger logger = LoggerFactory.getLogger(MasterAddressConfiguration.class);
	
	protected static final String DEFAULT_MASTER_HOSTSFILE = "config/hosts.csv";

	protected Option masterAddressOption;

	protected static final String DEFAULT_MASTER_ADDRESS = Configuration.DEFAULT_MASTER_ID + "@" +
		Configuration.DEFAULT_IP + ":" + Configuration.DEFAULT_MASTER_PORT;

	public final static String PROP_MASTER_ADDR = "master.address";
	
	protected static boolean masterServerInitialized = false;
	
	protected static Address masterAddress=null;
	
	/**
	 * 
	 * @param args
	 * @throws IOException
	 * @throws AddressParserException 
	 */
	public MasterAddressConfiguration(String[] args) throws ConfigurationException, AddressParserException, IOException {
		super(args);
		
		String host = compositeConfig.getString(PROP_MASTER_ADDR, DEFAULT_MASTER_ADDRESS);
		masterAddress = AddressParser.parseAddress(host);
		masterServerInitialized = true;
	}

	@Override
	protected void parseAdditionalOptions(String[] args) throws IOException {
				
		masterAddressOption = new Option("master", true, "Address of Master in format host:port:id");
		masterAddressOption.setArgName("address");
		options.addOption(masterAddressOption);
		
	}

	@Override
	protected void processAdditionalOptions() throws IOException {
		
		if (line.hasOption(masterAddressOption.getOpt()))
		{
			String addr = new String(line.getOptionValue(masterAddressOption.getOpt()));
			compositeConfig.setProperty(PROP_MASTER_ADDR, addr);
		}
		
	}

	/**
	 * Get address of master server.
	 * 
	 * @return address of Master or null
	 */
	public static Address getMasterAddress() {
		masterInitialized();
		
		return masterAddress;
	}

	
	
	static protected void masterInitialized() {
		baseInitialized();
		if (masterServerInitialized == false)
		{
			throw new IllegalStateException("MasterServerConfiguration not initialized  before use.");
		}
	}
}
