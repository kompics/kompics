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
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.address.Address;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.SimulationScenarioLoadException;
import se.sics.kompics.wan.util.HostsParser;
import se.sics.kompics.wan.util.HostsParserException;
import se.sics.kompics.wan.util.LocalIPAddressNotFound;

public class MasterConfiguration extends Configuration {

	private static final Logger logger = LoggerFactory.getLogger(MasterConfiguration.class);
	
	public static final String PROP_MASTER_SCENARIO_CLASSFILE = "master.scenario.classfile";
	public static final String PROP_MASTER_HOSTS_FILENAME = "master.hosts.filename";
	
	protected static final int DEFAULT_MASTER_MONITOR_ID = Integer.MAX_VALUE - 1;
	protected static final String DEFAULT_MASTER_HOSTSFILE = "config/hosts.csv";

	/**
	 * Ordered set (no duplicates)
	 */
	protected static TreeSet<Address> hosts=null;
	
	protected Address masterMonitorAddress=null;
	/********************************************************/
	/********* Helper fields ********************************/
	/********************************************************/
	
	protected Option scenarioClassfileOption;

	protected Option hostsFileOption;
	
	protected static boolean masterServerInitialized = false;
	
	/**
	 * 
	 * @param args
	 * @throws IOException
	 * @throws HostsParserException 
	 */
	public MasterConfiguration(String[] args) throws ConfigurationException, HostsParserException, IOException {
		super(args);
		
		String fName= compositeConfig.getString(PROP_MASTER_HOSTS_FILENAME, DEFAULT_MASTER_HOSTSFILE);
		hosts = HostsParser.parseHostsFile(fName);

		masterServerInitialized = true;
	}

	@Override
	protected void parseAdditionalOptions(String[] args) throws IOException {
				
		scenarioClassfileOption = new Option("scenarioclassfile", true, "File containing SimulationScenario class (experiment to be executed).");
		scenarioClassfileOption.setArgName("filename");
		options.addOption(scenarioClassfileOption);
		
		hostsFileOption = new Option("hostsfile", true,
				"Pathname to file containing a list of comma separated kompics "
						+ "addresses (format is host:port:id)");
		hostsFileOption.setArgName("filename");
		options.addOption(hostsFileOption);
	}

	@Override
	protected void processAdditionalOptions() throws IOException {
		
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
		masterServerInitialized();
		return configuration.compositeConfig.getInt(PROP_MONITOR_ID, DEFAULT_MASTER_MONITOR_ID);
	}

	@Override
	protected Address getMonitorServerAddress() {
		masterServerInitialized();
		if (masterMonitorAddress == null){
			masterMonitorAddress = new Address(getIp(), getPort(), getMonitorId());
		}			
		return masterMonitorAddress;
	}
	
	public static TreeSet<Address> getHosts() {
		masterServerInitialized();
		return hosts;
	}

	public static int getNumberHosts() {
		masterServerInitialized();
		return getHosts().size();
	}
	
	/**
	 * Gets the hosts from first to last (inclusive) from the hosts.csv file.
	 * @param first offset in list of hosts
	 * @param last offset in list of hosts
	 * @return
	 * @throws HostsParserException
	 * @throws FileNotFoundException
	 */
	public static TreeSet<Address> getHosts(int first, int last){
		masterServerInitialized();
		TreeSet<Address> h = getHosts();
		TreeSet<Address> subsetHosts = new TreeSet<Address>();
		Iterator<Address> iter = h.iterator();
		int count = 0;
		while (iter.hasNext() && (count <= last))
		{
			Address a = iter.next();
			if (count >= first)
			{
				subsetHosts.add(a);
			}
			count++;			
		}		
		return subsetHosts;
	}
	
	public static SimulationScenario getSimulationScenarioClass() throws SimulationScenarioLoadException
	{
		masterServerInitialized();
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
	
	
	static protected void masterServerInitialized() {
		baseInitialized();
		if (masterServerInitialized == false)
		{
			throw new IllegalStateException("MasterServerConfiguration not initialized  before use.");
		}
	}
}
