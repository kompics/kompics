package se.sics.kompics.wan.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.commons.cli.Option;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.address.Address;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.SimulationScenarioLoadException;
import se.sics.kompics.wan.util.HostsParser;
import se.sics.kompics.wan.util.HostsParserException;

public class PlanetLabConfiguration extends MasterConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(PlanetLabConfiguration.class);
	
	public static final String PROP_LOCAL_XML_RPC_PORT = "XmlRpcPort";
	
	public static final String PROP_HTTP_PROXY_HOST = "HttpProxyHost";
	public static final String PROP_HTTP_PROXY_PORT = "HttpProxyPort";
	public static final String PROP_HTTP_PROXY_USERNAME = "HttpProxyUsername";
	public static final String PROP_HTTP_PROXY_PASSWORD = "HttpProxyPassword";
	public static final String PROP_PLC_API_ADDRESS = "PlcApiAddress";
	
	
	protected static final int DEFAULT_LOCAL_XML_RPC_PORT = 8088;
	protected static final String DEFAULT_PLC_API_ADDRESS = "localhost";
	
	/********************************************************/
	/********* Helper fields ********************************/
	/********************************************************/
	
	protected Option localXmlRpcPortOption;

	protected Option plcApiOption;
	
	protected static boolean plInitialized = false;
	
	/**
	 * 
	 * @param args
	 * @throws IOException
	 * @throws HostsParserException 
	 */
	public PlanetLabConfiguration(String[] args) throws ConfigurationException, HostsParserException, IOException {
		super(args);
		
		plInitialized = true;
	}

	@Override
	protected void parseAdditionalOptions(String[] args) throws IOException {
		super.parseAdditionalOptions(args);
		localXmlRpcPortOption = new Option("localXmlRpcPort", true, "Local XML RPC port");
		localXmlRpcPortOption.setArgName("number");
		options.addOption(localXmlRpcPortOption);
		
		plcApiOption = new Option("plcApiAddress", true, "PLC API Address");
		plcApiOption.setArgName("address");
		options.addOption(plcApiOption);
	}

	@Override
	protected void processAdditionalOptions() throws IOException {
		super.processAdditionalOptions();
		if (line.hasOption(localXmlRpcPortOption.getOpt()))
		{
			int scf = new Integer(line.getOptionValue(localXmlRpcPortOption.getOpt()));
			compositeConfig.setProperty(PROP_LOCAL_XML_RPC_PORT, scf);
		}
		
		if (line.hasOption(plcApiOption.getOpt())) {
			String plc = new String(line.getOptionValue(plcApiOption.getOpt()));
			compositeConfig.setProperty(PROP_PLC_API_ADDRESS, plc);
		}
	}

	static public int getLocalXmlRpcPort()
	{
		return configuration.compositeConfig.getInt(PROP_LOCAL_XML_RPC_PORT, DEFAULT_LOCAL_XML_RPC_PORT);
	}

	static public String getPlcApiAddress()
	{
		return configuration.compositeConfig.getString(PROP_PLC_API_ADDRESS, DEFAULT_PLC_API_ADDRESS);
	}

	
	static protected void planetLabInitialized() {
		baseInitialized();
		if (plInitialized == false)
		{
			throw new IllegalStateException("MasterServerConfiguration not initialized  before use.");
		}
	}
}
