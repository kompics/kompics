package se.sics.kompics.kdld.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.util.List;
import java.util.Properties;

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
import org.apache.commons.configuration.SystemConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;
import se.sics.kompics.p2p.monitor.P2pMonitorConfiguration;
import se.sics.kompics.web.jetty.JettyWebServerInit;

public abstract class Configuration {
	private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

	
	public final static String PROP_BOOTSTRAP_PORT = "bootstrapPort";
	
	/*
	 * Non-publicly accessible
	 */
	protected static final int DEFAULT_BOOTSTRAP_PORT = 20002;
	protected static final String DEFAULT_BOOTSTRAP_IP = "localhost";
	protected static final int DEFAULT_BOOT_ID = Integer.MAX_VALUE;
	
	protected static final String DEFAULT_MONITOR_IP = "localhost";
	protected static final int DEFAULT_MONITOR_PORT = 20001;
	protected static final int DEFAULT_MONITOR_ID = Integer.MAX_VALUE;	

	protected static final int DEFAULT_EVICT_AFTER_SECS = 600;
	protected static final int DEFAULT_REFRESH_PERIOD = 10;
	protected static final int DEFAULT_RETRY_PERIOD = 500;
	protected static final int DEFAULT_RETRY_COUNT = 3;
	
	protected static final int DEFAULT_NUM_WORKERS = 1;
	protected static final int DEFAULT_NUM_PEERS = 1;
	
	protected static final int DEFAULT_NET_PORT = 20000;

	protected static final int DEFAULT_WEB_PORT = 8080;
	protected static final int DEFAULT_WEB_REQUEST_TIMEOUT_MS = 10000;
	protected static final int DEFAULT_WEB_THREADS = 2	;

	protected static final int DEFAULT_CONTROLLER_PORT = 9090;
	protected static final int DEFAULT_CONTROLLER_REQUEST_TIMEOUT_MS = 10000;
	protected static final String DEFAULT_CONTROLLER_IP = "localhost";

	
	/*
	 * Publicly accessible
	 */
	protected InetAddress ip = null;
	
	protected InetAddress controllerIP = null;

	protected int numPeers = DEFAULT_NUM_PEERS;

	protected int numWorkers = DEFAULT_NUM_WORKERS;

	protected int networkPort = DEFAULT_NET_PORT;

	protected int webPort = DEFAULT_WEB_PORT;

	protected int bootstrapId = DEFAULT_BOOT_ID;

	protected Address peer0Address;

	protected P2pMonitorConfiguration monitorConfiguration;

	protected int webRequestTimeout = DEFAULT_WEB_REQUEST_TIMEOUT_MS;

	protected int webThreads = DEFAULT_WEB_THREADS;

	protected String webAddress;

	protected String homepage;

	protected JettyWebServerInit jettyWebServerInit;

	protected BootstrapConfiguration bootConfiguration;

	protected List<Address> hosts;

	/**
	 * Singleton instance of configuration
	 */
	protected static Configuration configuration = null;
	
	protected CompositeConfiguration config = new CompositeConfiguration();

	
	/**
	 * Helper non-public fields
	 */
	protected Options options = new Options();

	protected CommandLine line;

	private enum ConfigType { CYCLON, CHORD };

	/**
	 * If configuration object already created, it returns
	 * the existing instance.
	 * Otherwise, creates and initializes an instance of a subclass of Configuration
	 * defined by configType parameter.
	 * 
	 * @param configType
	 * @param args
	 * @return
	 * @throws IOException
	 */
	public Configuration init(Class classname, String[] args) // Configuration.ConfigType configType
		throws IOException, ConfigurationException
	{
		config.addConfiguration(new SystemConfiguration());
		config.addConfiguration(new PropertiesConfiguration("bootstrap.properties"));
		config.addConfiguration(new PropertiesConfiguration("monitor.properties"));

		
		if (configuration != null)
		{
			return configuration;
		}
//		if (configType == Configuration.ConfigType.CYCLON)
//		{
//			configuration = new CyclonConfiguration(args);
//		}
//		else if (configType == Configuration.ConfigType.CHORD)
//		{
//			configuration = new CyclonConfiguration(args);
//		}
		Constructor c = classname.getConstructor(Array<String>);
		
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
		ip = InetAddress.getLocalHost();

		InetAddress bootstrapIP = ip;		
		int bootstrapPort = DEFAULT_BOOTSTRAP_PORT;
		long bootstrapEvictAfterSeconds = DEFAULT_EVICT_AFTER_SECS;
		long bootstrapRefreshPeriod = DEFAULT_REFRESH_PERIOD;
		long bootstrapClientRetryPeriod = DEFAULT_RETRY_PERIOD;
		int bootstrapClientRetryCount = DEFAULT_RETRY_COUNT;
		
		Properties bootstrapProperties = new Properties();
		InputStream bootstrapInputStream = Configuration.class
				.getResourceAsStream("bootstrap.properties");
		if (bootstrapInputStream != null) {
			bootstrapProperties.load(bootstrapInputStream);

			bootstrapIP = InetAddress.getByName(bootstrapProperties.getProperty(
					"bootstrap.server.ip", DEFAULT_BOOTSTRAP_IP));
			bootstrapPort = Integer.parseInt(bootstrapProperties.getProperty(
					"bootstrap.server.port", Integer.toString(DEFAULT_BOOTSTRAP_PORT)));
			bootstrapEvictAfterSeconds = Long.parseLong(bootstrapProperties.getProperty(
					"cache.evict.after", Integer.toString(DEFAULT_EVICT_AFTER_SECS)));
			bootstrapRefreshPeriod = 1000 * Long.parseLong(bootstrapProperties
					.getProperty("client.refresh.period", Integer.toString(DEFAULT_REFRESH_PERIOD)));
			bootstrapClientRetryPeriod = Long.parseLong(bootstrapProperties.getProperty(
					"client.retry.period", Integer.toString(DEFAULT_RETRY_PERIOD)));
			bootstrapClientRetryCount = Integer.parseInt(bootstrapProperties.getProperty(
					"client.retry.count", Integer.toString(DEFAULT_RETRY_COUNT)));
		}

		InetAddress monitorIP = ip;
		int monitorPort = DEFAULT_MONITOR_PORT;
		long monitorEvictAfterSeconds = DEFAULT_EVICT_AFTER_SECS;
		long monitorRefreshPeriod = DEFAULT_REFRESH_PERIOD;
		int monitorId = DEFAULT_MONITOR_ID;
		
		Properties monitorProperties = new Properties();
		InputStream monitorInputStream = Configuration.class
				.getResourceAsStream("monitor.properties");
		if (monitorInputStream != null) {

			monitorProperties.load(monitorInputStream);

			monitorIP = InetAddress.getByName(monitorProperties.getProperty("monitor.server.ip",
					DEFAULT_MONITOR_IP));
			monitorPort = Integer.parseInt(monitorProperties.getProperty("monitor.server.port",
					Integer.toString(DEFAULT_MONITOR_PORT)));
			monitorId = Integer.parseInt(monitorProperties.getProperty("monitor.server.id",
			Integer.toString(DEFAULT_MONITOR_ID)));

			monitorEvictAfterSeconds = Long.parseLong(monitorProperties.getProperty(
					"view.evict.after", Integer.toString(DEFAULT_EVICT_AFTER_SECS)));
			monitorRefreshPeriod = 1000 * Long.parseLong(monitorProperties
					.getProperty("client.refresh.period", Integer.toString(DEFAULT_REFRESH_PERIOD)));
		}

		// Users can override the default options from the command line

		Option hostsFileOption = new Option("hostsfile",true,
				"Pathname to file containing a list of comma separated kompics " +
				"addresses (format is host:port:id)");
		hostsFileOption.setArgName("hostsfile");
		options.addOption(hostsFileOption);

		Option bootstrapIpOption = new Option("bIp", true, "Bootstrap server ip address");
		bootstrapIpOption.setArgName("address");
		options.addOption(bootstrapIpOption);

		Option bootstrapPortOption = new Option("bPort", true, "Bootstrap server bootstrapPort");
		bootstrapPortOption.setArgName("number");
		options.addOption(bootstrapPortOption);

		Option numWorkersOption = new Option("workers", true, "Number of Workers to create");
		numWorkersOption.setArgName("number");
		options.addOption(numWorkersOption);

		Option monitorIpOption = new Option("mIp", true, "Peer Monitor server ip address");
		monitorIpOption.setArgName("address");
		options.addOption(monitorIpOption);

		Option monitorPortOption = new Option("mPort", true, "Peer Monitor port");
		monitorPortOption.setArgName("number");
		options.addOption(monitorPortOption);
		
		Option monitorIdOption = new Option("mId", true, "Peer Monitor id");
		monitorIdOption.setArgName("id");
		options.addOption(monitorIdOption);

		Option help = new Option("help", false, "Help message printed");
		options.addOption(help);

		Option debugOption = new Option("debug", false, "set log level to debug");
		options.addOption(debugOption);

		Option numPeersOption = new Option("peers", true, "Number of peers to simulate.");
		numPeersOption.setArgName("number");
		options.addOption(numPeersOption);

		Option monitorRefreshOption = new Option("mRefreshPeriod", true,
				"Client Monitor refresh Period");
		monitorRefreshOption.setArgName("seconds");
		options.addOption(monitorRefreshOption);

		Option bootstrapRefreshOption = new Option("bRefreshPeriod", true,
				"Bootstrap refresh Period");
		bootstrapRefreshOption.setArgName("seconds");
		options.addOption(bootstrapRefreshOption);

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

		if (line.hasOption(hostsFileOption.getOpt())) {
			String hostsFilename = new String(line.getOptionValue(hostsFileOption.getOpt()));
			try {
				hosts = HostsParser.parseHostsFile(hostsFilename);
			} catch (HostsParserException e) {
				e.printStackTrace();
				throw new IOException(e.getMessage());
			}
		}

		if (line.hasOption(bootstrapIpOption.getOpt())) {
			String bootstrapHost = new String(line.getOptionValue(bootstrapIpOption.getOpt()));
			bootstrapIP = InetAddress.getByName(bootstrapHost);
		}

		if (line.hasOption(bootstrapPortOption.getOpt())) {
			bootstrapPort = new Integer(line.getOptionValue(bootstrapPortOption.getOpt()));
		}

		if (line.hasOption(numPeersOption.getOpt())) {
			numPeers = new Integer(line.getOptionValue(numPeersOption.getOpt()));
		}

		if (line.hasOption(numWorkersOption.getOpt())) {
			numWorkers = new Integer(line.getOptionValue(numWorkersOption.getOpt()));
		}

		if (line.hasOption(monitorIpOption.getOpt())) {
			String monitorHost = new String(line.getOptionValue(monitorIpOption.getOpt()));
			monitorIP = InetAddress.getByName(monitorHost);
		}

		if (line.hasOption(monitorPortOption.getOpt())) {
			monitorPort = new Integer(line.getOptionValue(monitorPortOption.getOpt()));
		}

		if (line.hasOption(monitorRefreshOption.getOpt())) {
			monitorRefreshPeriod = new Long(line.getOptionValue(monitorRefreshOption.getOpt()));
		}

		if (line.hasOption(bootstrapRefreshOption.getOpt())) {
			bootstrapRefreshPeriod = new Long(line.getOptionValue(bootstrapRefreshOption.getOpt()));
		}

		if (line.hasOption(debugOption.getOpt())) {
			// XXX LEVEL = Level.DEBUG;
		}

		Address monitorAddress = new Address(monitorIP, monitorPort, monitorId);
		
		monitorConfiguration = new P2pMonitorConfiguration(monitorAddress, 
				monitorEvictAfterSeconds, monitorRefreshPeriod,	webPort);

		homepage = "<h2>Welcome to the Kompics Peer-to-Peer Framework!</h2>" + "<a href=\""
				+ webAddress + bootstrapId + "/" + "\">Bootstrap Server</a><br>" + "<a href=\""
				+ webAddress + getMonitorId() + "/" + "\">Monitor Server</a><br>";
		jettyWebServerInit = new JettyWebServerInit(ip, webPort, webRequestTimeout, webThreads,
				homepage);

		Address bootstrapServer = new Address(bootstrapIP, bootstrapPort, bootstrapId);

		bootConfiguration = new BootstrapConfiguration(bootstrapServer, 
				bootstrapEvictAfterSeconds, bootstrapClientRetryPeriod, bootstrapClientRetryCount, 
				bootstrapRefreshPeriod, webPort);

		webAddress = "http://" + ip.getHostAddress() + ":" + webPort + "/";


		peer0Address = new Address(ip, networkPort, 0);

	}
	
	public abstract void parseAdditionalOptions(String[] args) throws IOException;

	public abstract void processAdditionalOptions() throws IOException;

	/**
	 * @param options
	 */
	protected void help(String message, Options options) {
		HelpFormatter formatter = new HelpFormatter();

		String applicationName = System.getProperty("app.name", "bootstrap-server");

		StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);

		formatter.printHelp(writer, HelpFormatter.DEFAULT_WIDTH, applicationName, "", options,
				HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, "");

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

	protected abstract Address getMonitorServerAddress();

	protected abstract int getMonitorId();
	
	public static List<Address> getHosts() {
		return configuration.hosts;
	}
	
	public static BootstrapConfiguration getBootConfiguration() {
		return configuration.bootConfiguration;
	}
	
	public static P2pMonitorConfiguration getMonitorConfiguration() {
		return configuration.monitorConfiguration;
	}
	
	public static int getNumPeers() {
		return configuration.numPeers;
	}
	
	public static InetAddress getIp() {
		return configuration.ip;
	}
	
	public static String getWebAddress() {
		return configuration.webAddress;
	}
	
	public JettyWebServerInit getJettyWebServerInit() {
		return configuration.jettyWebServerInit;
	}
}
