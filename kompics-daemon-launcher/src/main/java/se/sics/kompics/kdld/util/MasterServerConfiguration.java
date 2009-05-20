package se.sics.kompics.kdld.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.address.Address;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.SimulationScenarioLoadException;

public class MasterServerConfiguration extends Configuration {

	private static final Logger logger = LoggerFactory.getLogger(MasterServerConfiguration.class);
	
	public static String PROP_MASTER_CONFIG_PROPS_FILE = "config/master.properties";
	
	public final static String PROP_MASTER_SCENARIO_CLASSFILE = "master.scenario.classfile";
	public final static String PROP_MASTER_HOSTS_FILENAME = "master.hosts.filename";
	
	protected final static int DEFAULT_MASTER_MONITOR_ID = Integer.MAX_VALUE - 1;
	protected final static String DEFAULT_MASTER_HOSTSFILE = "config/hosts.csv";

	
	protected Address masterMonitorAddress = null;

	protected static List<Address> hosts;
	
	/********************************************************/
	/********* Helper fields ********************************/
	/********************************************************/
	protected Option masterMonitorIdOption;
	
	protected Option masterConfigFileOption;
	
	protected Option scenarioClassfileOption;

	protected Option hostsFileOption;
	
	protected PropertiesConfiguration masterConfig;
	
	/**
	 * 
	 * @param args
	 * @throws IOException
	 */
	public MasterServerConfiguration(String[] args) throws IOException, ConfigurationException {
		super(args);

	}

	@Override
	protected void parseAdditionalOptions(String[] args) throws IOException {
		
		masterMonitorIdOption = new Option("monitorid", true, "Cyclon monitor-id");
		masterMonitorIdOption.setArgName("id");
		options.addOption(masterMonitorIdOption);
		
		
		masterConfigFileOption = new Option("masterprops", true, "Master properties file.");
		masterConfigFileOption.setArgName("masterprops");
		options.addOption(masterConfigFileOption);
		
		
		scenarioClassfileOption = new Option("scenarioclassfile", true, "File containing SimulationScenario class (experiment to be executed).");
		scenarioClassfileOption.setArgName("scenarioclassfile");
		options.addOption(scenarioClassfileOption);
		
		hostsFileOption = new Option("hostsfile", true,
				"Pathname to file containing a list of comma separated kompics "
						+ "addresses (format is host:port:id)");
		hostsFileOption.setArgName("hostsfile");
		options.addOption(hostsFileOption);
	}

	@Override
	protected void processAdditionalOptions() throws IOException {

		if (line.hasOption(masterConfigFileOption.getOpt()))
		{
			PROP_MASTER_CONFIG_PROPS_FILE =
				new String(line.getOptionValue(masterConfigFileOption.getOpt()));
		}

		try {
			masterConfig = new PropertiesConfiguration(PROP_MASTER_CONFIG_PROPS_FILE);
			masterConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
			configuration.compositeConfig.addConfiguration(masterConfig);
		}
		catch (ConfigurationException e)
		{
			logger.warn("Configuration file for cyclon not found, using default values: " + PROP_MASTER_CONFIG_PROPS_FILE);
		}

		
		if (line.hasOption(masterMonitorIdOption.getOpt()))
		{
			int cyclonMonitorId = new Integer(line.getOptionValue(masterMonitorIdOption.getOpt()));
			configuration.compositeConfig.setProperty(PROP_MONITOR_ID, cyclonMonitorId);
		}
		
		if (line.hasOption(scenarioClassfileOption.getOpt()))
		{
			String scf = new String(line.getOptionValue(scenarioClassfileOption.getOpt()));
			configuration.compositeConfig.setProperty(PROP_MASTER_SCENARIO_CLASSFILE, scf);
		}
		
		if (line.hasOption(hostsFileOption.getOpt())) {
			String hostsFilename = new String(line.getOptionValue(hostsFileOption.getOpt()));
			configuration.compositeConfig.setProperty(PROP_MASTER_HOSTS_FILENAME, hostsFilename);
		}
	}

	@Override
	protected int getMonitorId() {
		testInitialized();
		return configuration.compositeConfig.getInt(PROP_MONITOR_ID, DEFAULT_MASTER_MONITOR_ID);
	}

	@Override
	protected Address getMonitorServerAddress() throws LocalIPAddressNotFound {
		testInitialized();
		if (masterMonitorAddress == null){
			masterMonitorAddress = new Address(getIp(), getPort(), getMonitorId());
		}			
		return masterMonitorAddress;
	}
	
	public static List<Address> getHosts() throws HostsParserException, FileNotFoundException{
		testInitialized();
		String fName= configuration.compositeConfig.getString(
				PROP_MASTER_HOSTS_FILENAME, DEFAULT_MASTER_HOSTSFILE);
		hosts = HostsParser.parseHostsFile(fName);
		return hosts;
	}
	
	public static SimulationScenario getSimulationScenarioClass() throws SimulationScenarioLoadException
	{
		testInitialized();
		SimulationScenario scenario = null;
		String sName = configuration.compositeConfig.getString(
				PROP_MASTER_SCENARIO_CLASSFILE);
		if (sName == null)
		{
			throw new SimulationScenarioLoadException("No simulation scenario class file was specified.");
		}
		try {
			File f = new File(sName);
			FileInputStream scenarioFile = new FileInputStream(f);
			ObjectInputStream ois = new ObjectInputStream(scenarioFile);
			scenario = (SimulationScenario) ois.readObject();
			ois.close();
		} catch (FileNotFoundException e) {
			throw new SimulationScenarioLoadException(e.getMessage());
		} catch (IOException e) {
			throw new SimulationScenarioLoadException(e.getMessage());
		} catch (ClassNotFoundException e) {
			throw new SimulationScenarioLoadException(e.getMessage());
		}
		return scenario;
	}
	
}
