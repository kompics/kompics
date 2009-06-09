package se.sics.kompics.wan.config;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;
import se.sics.kompics.p2p.monitor.P2pMonitorConfiguration;
import se.sics.kompics.wan.util.LocalNetworkConfiguration;
import se.sics.kompics.web.jetty.JettyWebServerInit;


/**
 * This class is not thread-safe for writing, although it is thread-safe for
 * reading.
 * 
 * @author jdowling
 * 
 */
public abstract class Configuration {
	private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

	public static final String BOOTSTRAP_CONFIG_FILE = "config/bootstrap.properties";
	public static final String MONITOR_CONFIG_FILE = "config/monitor.properties";
	public static final String EXPERIMENT_CONFIG_FILE = "config/experiment.properties";

	public static final String PROP_IP = "ip";
	public static final String PROP_PORT = "port";
	public static final String PROP_ID = "id";

	public static final String PROP_BOOTSTRAP_PORT = "bootstrap.server.port";
	public static final String PROP_BOOTSTRAP_IP = "bootstrap.server.ip";
	public static final String PROP_BOOTSTRAP_EVICT_AFTER = "bootstrap.evict.after";
	public static final String PROP_BOOTSTRAP_REFRESH_PERIOD = "client.refresh.period";
	public static final String PROP_BOOTSTRAP_RETRY_PERIOD = "client.retry.period";
	public static final String PROP_BOOTSTRAP_RETRY_COUNT = "client.retry.count";

	public static final String PROP_MONITOR_IP = "monitor.server.ip";
	public static final String PROP_MONITOR_PORT = "monitor.server.port";
	public static final String PROP_MONITOR_ID = "monitor.server.id";
	public static final String PROP_MONITOR_EVICT_AFTER = "monitor.evict.after";
	public static final String PROP_MONITOR_REFRESH_PERIOD = "client.refresh.period";
	public static final String PROP_MONITOR_RETRY_PERIOD = "client.retry.period";
	public static final String PROP_MONITOR_RETRY_COUNT = "client.retry.count";

	public static final String PROP_EXPERIMENT_IDSPACE_SIZE = "idspace.size";
	public static final String PROP_EXPERIMENT_NUM_PEERS = "peers.number";
	public static final String PROP_EXPERIMENT_REPO_ID = "repo.id";
	public static final String PROP_EXPERIMENT_REPO_URL = "repo.url";
	public static final String PROP_EXPERIMENT_NUM_WORKERS = "number.workers";	
	/*
	 * Non-publicly accessible
	 */
	protected static final String DEFAULT_IP = LocalNetworkConfiguration.findLocalHostAddress();
	public static final int DEFAULT_PORT = 2323;
	public static final int DEFAULT_ID = 0;

	protected static final String DEFAULT_BOOTSTRAP_IP = "localhost";
	protected static final int DEFAULT_BOOTSTRAP_PORT = 20002;
	protected static final int DEFAULT_BOOTSTRAP_ID = Integer.MAX_VALUE;

	protected static final String DEFAULT_MONITOR_IP = "localhost";
	protected static final int DEFAULT_MONITOR_PORT = 20001;
	protected static final int DEFAULT_MONITOR_ID = Integer.MAX_VALUE - 1;

	protected static final int DEFAULT_EVICT_AFTER = 300*1000;
	protected static final int DEFAULT_REFRESH_PERIOD = 30*1000;
	protected static final int DEFAULT_RETRY_PERIOD = 30*1000;
	protected static final int DEFAULT_RETRY_COUNT = 3;

	protected static final int DEFAULT_WEB_PORT = 8080;
	protected static final int DEFAULT_WEB_REQUEST_TIMEOUT_MS = 30*1000;
	protected static final int DEFAULT_WEB_THREADS = 2;

//	protected static final int DEFAULT_CONTROLLER_PORT = 9090;
//	protected static final int DEFAULT_CONTROLLER_REQUEST_TIMEOUT_MS = 10000;
//	protected static final String DEFAULT_CONTROLLER_IP = "localhost";

	protected static final String DEFAULT_EXPERIMENT_IDSPACE_SIZE = "10000";
	protected static final int DEFAULT_EXPERIMENT_NUM_PEERS = 1;
	protected static final String DEFAULT_EXPERIMENT_REPO_ID = "";
	protected static final String DEFAULT_EXPERIMENT_REPO_URL = "";
	protected static final int DEFAULT_EXPERIMENT_NUM_WORKERS = 1;
	
	public static final String OPT_PEERS = "peers";
	public static final String OPT_IDSPACE = "idspace";
	
	protected static final String VAL_ADDRESS = "address";
	protected static final String VAL_NUMBER = "number";
	protected static final String VAL_PERIOD_SECS = "seconds";
	protected static final String VAL_PERIOD_MILLISECS = "milliseconds";
	

	
	protected InetAddress ip = null;

	protected int webPort = DEFAULT_WEB_PORT;

	protected int bootstrapId = DEFAULT_BOOTSTRAP_ID;

	protected Address peer0Address = null;

	protected P2pMonitorConfiguration monitorConfiguration = null;

	protected int webRequestTimeout = DEFAULT_WEB_REQUEST_TIMEOUT_MS;

	protected int webThreads = DEFAULT_WEB_THREADS;

	protected String webAddress;

	protected String homepage;

	protected JettyWebServerInit jettyWebServerInit = null;

	protected BootstrapConfiguration bootConfiguration = null;

	/**
	 * Singleton instance of configuration
	 */
	protected static Configuration configuration = null;

	protected PropertiesConfiguration bootstrapConfig;
	protected PropertiesConfiguration monitorConfig;
	protected PropertiesConfiguration experimentConfig;

	protected CompositeConfiguration compositeConfig = new CompositeConfiguration();

	/**
	 * Helper non-public fields
	 */
	protected Options options = new Options();

	protected CommandLine line;

	
	/**
	 * If configuration object already created, it returns the existing
	 * instance. Otherwise, creates and initializes an instance of a subclass of
	 * Configuration defined by configType parameter.
	 * 
	 * @param configType
	 * @param args
	 * @return
	 * @throws IOException
	 */
	public static final synchronized Configuration init(String[] args,
			Class<? extends Configuration>... classname) throws ConfigurationException {

		// if (configuration != null) {
		// return configuration;
		// }

		try {
			// Create instance of subclass of Configuration and call its
			// constructor (String[]) using reflection
			// XXX improve exception processing
			Constructor<? extends Configuration> constructor = classname[0]
					.getConstructor(String[].class);
			configuration = constructor.newInstance((Object) args);
		} catch (SecurityException e) {
			throw new ConfigurationException(e.getMessage());
		} catch (NoSuchMethodException e) {
			throw new ConfigurationException(e.getMessage());
		} catch (InstantiationException e) {
			throw new ConfigurationException(e.getMessage());
		} catch (InvocationTargetException e) {
			throw new ConfigurationException(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new ConfigurationException(e.getMessage());
		} catch (IllegalAccessException e) {
			throw new ConfigurationException(e.getMessage());
		}

		return configuration;
	}

	/**
	 * You should call this constructor from the main method of your Main class,
	 * with the args parameters from your main method.
	 * 
	 * @param args
	 * @return
	 * @throws IOException
	 */
	protected Configuration(String[] args) throws IOException {

		try {
			bootstrapConfig = new PropertiesConfiguration(BOOTSTRAP_CONFIG_FILE);
			bootstrapConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
			compositeConfig.addConfiguration(bootstrapConfig);
		} catch (ConfigurationException e) {
			logger.warn("Bootstrap configuration file not found, using default values: "
					+ BOOTSTRAP_CONFIG_FILE);
		}

		try {
			monitorConfig = new PropertiesConfiguration(MONITOR_CONFIG_FILE);
			monitorConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
			compositeConfig.addConfiguration(monitorConfig);
		} catch (ConfigurationException e) {
			logger.warn("Monitor configuration file not found, using default values: "
					+ MONITOR_CONFIG_FILE);
		}
		
		try {
			experimentConfig = new PropertiesConfiguration(EXPERIMENT_CONFIG_FILE);
			experimentConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
			compositeConfig.addConfiguration(experimentConfig);
		} catch (ConfigurationException e) {
			logger.warn("Experiment configuration file not found, using default values: "
					+ EXPERIMENT_CONFIG_FILE);
		}

		// Users can override the default options from the command line
		Option ipOption = new Option("ip", true,
				"IP address (or hostname) for Kompics instance to listen on");
		ipOption.setArgName("address");
		options.addOption(ipOption);

		Option portOption = new Option("port", true, "Port for Kompics instance to listen on");
		portOption.setArgName(VAL_NUMBER);
		options.addOption(portOption);

		Option bootstrapIpOption = new Option("bIp", true, "Bootstrap server ip address");
		bootstrapIpOption.setArgName(VAL_ADDRESS);
		options.addOption(bootstrapIpOption);

		Option bootstrapPortOption = new Option("bPort", true, "Bootstrap server bootstrapPort");
		bootstrapPortOption.setArgName(VAL_NUMBER);
		options.addOption(bootstrapPortOption);

		Option numWorkersOption = new Option("workers", true, "Number of Workers to create");
		numWorkersOption.setArgName(VAL_NUMBER);
		options.addOption(numWorkersOption);

		Option monitorIpOption = new Option("mIp", true, "Peer Monitor server ip address");
		monitorIpOption.setArgName("address");
		options.addOption(monitorIpOption);

		Option monitorPortOption = new Option("mPort", true, "Peer Monitor port");
		monitorPortOption.setArgName(VAL_ADDRESS);
		options.addOption(monitorPortOption);

		Option monitorIdOption = new Option("mId", true, "Peer Monitor id");
		monitorIdOption.setArgName(VAL_NUMBER);
		options.addOption(monitorIdOption);

		Option help = new Option("help", false, "Help message printed");
		options.addOption(help);

		Option numPeersOption = new Option(OPT_PEERS, true, "Number of peers to simulate.");
		numPeersOption.setArgName(VAL_NUMBER);
		options.addOption(numPeersOption);

		Option monitorRefreshOption = new Option("mRefreshPeriod", true,
				"Client Monitor refresh Period");
		monitorRefreshOption.setArgName(VAL_PERIOD_SECS);
		options.addOption(monitorRefreshOption);

		Option bootstrapRefreshOption = new Option("bRefreshPeriod", true,
				"Bootstrap refresh Period");
		bootstrapRefreshOption.setArgName(VAL_PERIOD_SECS);
		options.addOption(bootstrapRefreshOption);
		
		Option idSpaceSizeOption = new Option(OPT_IDSPACE, true, "the size of the identifier space");
		idSpaceSizeOption.setArgName(VAL_NUMBER);
		options.addOption(idSpaceSizeOption);

		Option repoIdOption = new Option("repoId", true, "the maven repository id (default: sics-snapshot)");
		repoIdOption.setArgName("id");
		options.addOption(repoIdOption);
		
		Option repoUrlOption = new Option("repoUrl", true, "the maven repository url (default: http://kompics.sics.se/maven/snapshotrepository)");
		repoUrlOption.setArgName("url");
		options.addOption(repoUrlOption);

		
		// implemented by subclass
		parseAdditionalOptions(args);

		CommandLineParser parser = new GnuParser();
		try {
			line = parser.parse(options, args);
		} catch (ParseException e) {
			help("Parsing failed.  " + e.getMessage(), options);
			throw new IOException(e.getMessage());
		}
		// implemented by subclass
		processAdditionalOptions();

		if (line.hasOption(help.getOpt())) {
			help("", options);
		}

		if (line.hasOption(ipOption.getOpt())) {
			String host = new String(line.getOptionValue(ipOption.getOpt()));
			compositeConfig.setProperty(PROP_IP, host);
		}
		if (line.hasOption(portOption.getOpt())) {
			int port = new Integer(line.getOptionValue(portOption.getOpt()));
			compositeConfig.setProperty(PROP_PORT, port);
		}

		if (line.hasOption(bootstrapIpOption.getOpt())) {
			String bootstrapHost = new String(line.getOptionValue(bootstrapIpOption.getOpt()));
			compositeConfig.setProperty(PROP_BOOTSTRAP_IP, bootstrapHost);
		}
		if (line.hasOption(bootstrapPortOption.getOpt())) {
			int bootstrapPort = new Integer(line.getOptionValue(bootstrapPortOption.getOpt()));
			compositeConfig.setProperty(PROP_BOOTSTRAP_PORT, bootstrapPort);
		}
		if (line.hasOption(bootstrapRefreshOption.getOpt())) {
			long bootstrapRefreshPeriod = new Long(line.getOptionValue(bootstrapRefreshOption
					.getOpt()));
			compositeConfig.setProperty(PROP_BOOTSTRAP_REFRESH_PERIOD, bootstrapRefreshPeriod);
		}

		if (line.hasOption(numPeersOption.getOpt())) {
			int numPeers = new Integer(line.getOptionValue(numPeersOption.getOpt()));
			compositeConfig.setProperty(PROP_EXPERIMENT_NUM_PEERS, numPeers);
		}

		if (line.hasOption(numWorkersOption.getOpt())) {
			int numWorkers = new Integer(line.getOptionValue(numWorkersOption.getOpt()));
			compositeConfig.setProperty(PROP_EXPERIMENT_NUM_WORKERS, numWorkers);
		}

		if (line.hasOption(monitorIpOption.getOpt())) {
			String monitorHost = new String(line.getOptionValue(monitorIpOption.getOpt()));
			compositeConfig.setProperty(PROP_MONITOR_IP, monitorHost);
		}

		if (line.hasOption(monitorPortOption.getOpt())) {
			int monitorPort = new Integer(line.getOptionValue(monitorPortOption.getOpt()));
			compositeConfig.setProperty(PROP_MONITOR_PORT, monitorPort);
		}

		if (line.hasOption(monitorRefreshOption.getOpt())) {
			long monitorRefreshPeriod = new Long(line.getOptionValue(monitorRefreshOption.getOpt()));
			compositeConfig.setProperty(PROP_MONITOR_REFRESH_PERIOD, monitorRefreshPeriod);
		}

		if (line.hasOption(idSpaceSizeOption.getOpt()))
		{
			int idss = new Integer(line.getOptionValue(idSpaceSizeOption.getOpt()));
			compositeConfig.setProperty(Configuration.PROP_EXPERIMENT_IDSPACE_SIZE, idss);
		}

		if (line.hasOption(repoIdOption.getOpt()))
		{
			String repoId = new String(line.getOptionValue(repoIdOption.getOpt()));
			compositeConfig.setProperty(PROP_EXPERIMENT_REPO_ID, repoId);
		}
		
		if (line.hasOption(repoUrlOption.getOpt()))
		{
			String repoUrl = new String(line.getOptionValue(repoUrlOption.getOpt()));
			compositeConfig.setProperty(PROP_EXPERIMENT_REPO_URL, repoUrl);
		}
		

		ip = InetAddress.getByName(compositeConfig.getString(
						PROP_IP, DEFAULT_IP));

		webAddress = "http://" + compositeConfig.getString(
				PROP_IP, DEFAULT_IP) + ":"
		+ webPort + "/";

		bootConfiguration = setupBootstrapConfig();

		monitorConfiguration = setupMonitorConfig();
		
		homepage = "<h2>Welcome to the Kompics Peer-to-Peer Framework!</h2>"
			+ "<a href=\"" + webAddress
			+ bootConfiguration.getBootstrapServerAddress().getId() + "/"
			+ "\">Bootstrap Server</a><br>" + "<a href=\"" + webAddress
			+ monitorConfiguration.getMonitorServerAddress().getId() + "/"
			+ "\">Monitor Server</a><br>";


	}

	abstract protected void parseAdditionalOptions(String[] args) throws IOException;

	abstract protected void processAdditionalOptions() throws IOException;

	/**
	 * @param options
	 */
	protected void help(String message, Options options) {
		HelpFormatter formatter = new HelpFormatter();

		String applicationName = System.getProperty("app.name", "kompics");

		StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);

		formatter.printHelp(writer, HelpFormatter.DEFAULT_WIDTH, applicationName, "", options,
				HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, "");

		// XXX add the default values for parameters
		writer.close();

		displayHelper(message, stringWriter.getBuffer().toString());
		System.exit(1);
	}

	protected void displayHelper(String message, String usage) {
		if (message != null) {
			logger.info(message);
		}
		logger.info(usage);
	}

	public static String getHomepage() {
		baseInitialized();
		return configuration.homepage;
	}

	/**
	 * Implemented by monitor program for concrete overlay
	 * 
	 * @return id of monitor server for concrete overlay
	 */
	protected abstract Address getMonitorServerAddress();

	/**
	 * Implemented by monitor program for concrete overlay
	 * 
	 * @return id of monitor server for concrete overlay
	 */
	protected abstract int getMonitorId();

	public static BootstrapConfiguration getBootConfiguration() {
		baseInitialized();
		return configuration.bootConfiguration;
	}

	private BootstrapConfiguration setupBootstrapConfig() throws UnknownHostException {
		String bootstrapHost = compositeConfig.getString(PROP_BOOTSTRAP_IP,
				DEFAULT_BOOTSTRAP_IP);
		InetAddress bootstrapIP = InetAddress.getByName(bootstrapHost);

		int bootstrapPort = compositeConfig.getInt(PROP_BOOTSTRAP_PORT,
				DEFAULT_BOOTSTRAP_PORT);

		Address bootstrapAddress = new Address(bootstrapIP, bootstrapPort,
				bootstrapId);
		return new BootstrapConfiguration(bootstrapAddress, compositeConfig.getInt(
				PROP_BOOTSTRAP_EVICT_AFTER, DEFAULT_EVICT_AFTER),
				compositeConfig.getInt(PROP_BOOTSTRAP_RETRY_PERIOD,
						DEFAULT_RETRY_PERIOD), compositeConfig.getInt(
						PROP_BOOTSTRAP_RETRY_COUNT, DEFAULT_RETRY_COUNT),
				compositeConfig.getInt(PROP_BOOTSTRAP_REFRESH_PERIOD,
						DEFAULT_REFRESH_PERIOD), webPort);
	}

	public static P2pMonitorConfiguration getMonitorConfiguration() {
		baseInitialized();
		return configuration.monitorConfiguration;
	}

	private P2pMonitorConfiguration setupMonitorConfig() throws UnknownHostException {
		String monitorHost = compositeConfig.getString(PROP_MONITOR_IP,
				DEFAULT_MONITOR_IP);
		InetAddress monitorIP = InetAddress.getByName(monitorHost);
		
		int monitorPort = compositeConfig.getInt(PROP_MONITOR_PORT,
				DEFAULT_MONITOR_PORT);
		int monitorId = compositeConfig.getInt(PROP_MONITOR_ID, DEFAULT_MONITOR_ID);

		Address monitorAddress = new Address(monitorIP, monitorPort, monitorId);
		return new P2pMonitorConfiguration(monitorAddress, compositeConfig.getInt(
				PROP_MONITOR_EVICT_AFTER, DEFAULT_EVICT_AFTER), compositeConfig
				.getInt(PROP_MONITOR_REFRESH_PERIOD, DEFAULT_REFRESH_PERIOD), webPort);
	}

	public static InetAddress getIp() {
		baseInitialized();
		return configuration.ip;
	}

	public static int getPort() {
		baseInitialized();
		return configuration.compositeConfig.getInt(PROP_PORT, DEFAULT_PORT);
	}

	public static int getId() {
		baseInitialized();
		return DEFAULT_ID;
	}

	public static String getWebAddress() {
		baseInitialized();
		return configuration.webAddress;
	}

	public static JettyWebServerInit getJettyWebServerInit() {
		baseInitialized();
		if (configuration.jettyWebServerInit == null) {
			configuration.jettyWebServerInit = new JettyWebServerInit(getIp(), getWebPort(),
					configuration.webRequestTimeout, configuration.webThreads, getHomepage());
		}

		return configuration.jettyWebServerInit;
	}

	public static int getWebPort() {
		baseInitialized();
		return configuration.webPort;
	}

	public static Address getPeer0Address() {
		baseInitialized();
		if (configuration.peer0Address == null) {
			configuration.peer0Address = new Address(getIp(), getPort(), getId());
		}
		return configuration.peer0Address;
	}

	public static BigInteger getIdSpaceSize() {
		baseInitialized();
		String idSpaceSize = configuration.compositeConfig.getString(Configuration.PROP_EXPERIMENT_IDSPACE_SIZE, 
				Configuration.DEFAULT_EXPERIMENT_IDSPACE_SIZE);
		return new BigInteger(idSpaceSize);
	}

	public static int getNumPeers() {
		baseInitialized();
		return configuration.compositeConfig.getInt(PROP_EXPERIMENT_NUM_PEERS, DEFAULT_EXPERIMENT_NUM_PEERS);
	}
	
	public static String getDefaultRepoId() {
		baseInitialized();
		return configuration.compositeConfig.getString(PROP_EXPERIMENT_REPO_ID, 
				DEFAULT_EXPERIMENT_REPO_ID);
	}
	
	public static String getDefaultRepoUrl() {
		baseInitialized();
		return configuration.compositeConfig.getString(PROP_EXPERIMENT_REPO_URL, 
				DEFAULT_EXPERIMENT_REPO_URL);
	}
	
	protected static void baseInitialized() {
		if (configuration == null) {
			throw new IllegalStateException(
					"Configuration not initialized. You must call init method first, before other methods.");
		}
	}
	
	public static void printConfigurationValues()
	{
		logger.info("============= Start Configuration Values =============================");
		logger.info("Local Address (Peer0Address):\t {}",getPeer0Address());
		logger.info("Web Address:\t\t\t\t{}:{}", getWebAddress(), getWebPort());
		logger.info("Number of Peers:\t\t{}", getNumPeers());
		logger.info("Repo id:\t\t{}", getDefaultRepoId());
		logger.info("Repo url:\t\t{}", getDefaultRepoUrl());
		logger.info("Bootstrap Server:\t\t{}", getBootConfiguration().getBootstrapServerAddress());
		logger.info("Monitor Server:\t\t{}", getMonitorConfiguration().getMonitorServerAddress());
		logger.info("Web Server:\t\t\t{}", getJettyWebServerInit().getHomePage());
		logger.info("");
		logger.info("");
		logger.info("");
		logger.info("");
		logger.info("");
		logger.info("============= End Configuration Values =============================");
	}
}
