package se.sics.kompics.wan.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.PropertyConfigurator;

import se.sics.kompics.ChannelFilter;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Kompics;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;
import se.sics.kompics.p2p.bootstrap.server.BootstrapServer;
import se.sics.kompics.p2p.bootstrap.server.BootstrapServerInit;
import se.sics.kompics.p2p.epfd.EventuallyPerfectFailureDetector;
import se.sics.kompics.p2p.epfd.diamondp.FailureDetector;
import se.sics.kompics.p2p.monitor.P2pMonitorConfiguration;
import se.sics.kompics.p2p.monitor.cyclon.server.CyclonMonitorServer;
import se.sics.kompics.p2p.monitor.cyclon.server.P2pMonitorServerInit;
import se.sics.kompics.p2p.simulator.cyclon.CyclonSimulator;
import se.sics.kompics.p2p.simulator.cyclon.CyclonSimulatorPort;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.SimulationScenarioLoadException;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.config.CyclonConfiguration;
import se.sics.kompics.wan.daemon.Daemon;
import se.sics.kompics.wan.master.Master;
import se.sics.kompics.wan.util.LocalIPAddressNotFound;
import se.sics.kompics.web.Web;
import se.sics.kompics.web.WebRequest;
import se.sics.kompics.web.jetty.JettyWebServer;

public final class CyclonWanMasterMain extends ComponentDefinition {

	private static String HOSTS_FILE = "hosts.csv";
	private static List<String> listHosts = new ArrayList<String>();

	static {
		PropertyConfigurator.configureAndWatch("log4j.properties");
	}

	private void host(String ip) {
		listHosts.add(ip);
	}

	{
		host("evgsics1.sics.se");
		host("evgsics2.sics.se");
		host("evgsics3.sics.se");
		host("evgsics4.sics.se");
	}

	private static SimulationScenario scenario;

	private static void readHostsFile() throws FileNotFoundException, IOException {
		File file = new File(HOSTS_FILE);
		BufferedReader bufRdr = new BufferedReader(new FileReader(file));
		String line = null;
		int row = 0;
		int col = 0;

		// read each line of text file
		while ((line = bufRdr.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line, ",");
			while (st.hasMoreTokens()) {
				// get next token and store it in the array
				listHosts.add(st.nextToken());
				col++;
			}
			row++;
		}
		// close the file
		bufRdr.close();
	}

	public static void main(String[] args) {

		try {
			readHostsFile();
			Kompics.createAndStart(CyclonWanMasterMain.class, 1);
		} catch (FileNotFoundException e) {
			System.err.println("Could not find hosts file:" + HOSTS_FILE);
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Problem when reading hosts file:" + HOSTS_FILE);
			e.printStackTrace();
		}

	}

	public CyclonWanMasterMain() throws UnknownHostException, InterruptedException {
		// Master.setSimulationPortType(CyclonSimulatorPort.class);
		// create
		Component master = create(Master.class);
		Component jettyWebServer = create(JettyWebServer.class);
		Component bootstrapServer = create(BootstrapServer.class);
		Component monitorServer = create(CyclonMonitorServer.class);
		Component cyclonSimulator = create(CyclonSimulator.class);

		Component fd = create(FailureDetector.class);

		// final Configuration config = new Configuration();
		String[] args = {};
		CyclonConfiguration cyclonConfiguration = null;
		try {
			cyclonConfiguration = (CyclonConfiguration) Configuration.init(args,
					CyclonConfiguration.class);
		} catch (ConfigurationException e1) {
			e1.printStackTrace();
		}
		final BootstrapConfiguration bootConfiguration = Configuration.getBootConfiguration();
		final P2pMonitorConfiguration monitorConfiguration = Configuration
				.getMonitorConfiguration();

		try {
			scenario = SimulationScenario.load(System.getProperty(Daemon.SCENARIO_FILENAME));
		} catch (SimulationScenarioLoadException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		System.out.println("For web access please go to " + cyclonConfiguration.getWebAddress());

		// trigger(new MasterInit(scenario, new KingLatencyMap()),
		// master.getControl());
		trigger(cyclonConfiguration.getJettyWebServerInit(), jettyWebServer.getControl());
		trigger(new BootstrapServerInit(bootConfiguration), bootstrapServer.getControl());
		trigger(new P2pMonitorServerInit(monitorConfiguration), monitorServer.getControl());

		// XXX - change CyclonSimulatorInit to include new defn of
		// CyclonConfiguration
		// trigger(
		// new CyclonSimulatorInit(bootConfiguration,
		// monitorConfiguration, cyclonConfiguration,
		// cyclonConfiguration.getPeer0Address()),
		// cyclonSimulator.getControl());

		final class MessageDestinationFilter extends ChannelFilter<Message, Address> {
			public MessageDestinationFilter(Address address) {
				super(Message.class, address, true);
			}

			public Address getValue(Message event) {
				return event.getDestination();
			}
		}
		final class WebRequestDestinationFilter extends ChannelFilter<WebRequest, Integer> {
			public WebRequestDestinationFilter(Integer destination) {
				super(WebRequest.class, destination, false);
			}

			public Integer getValue(WebRequest event) {
				return event.getDestination();
			}
		}

		// connect
		connect(bootstrapServer.getNegative(Network.class), master.getPositive(Network.class),
				new MessageDestinationFilter(bootConfiguration.getBootstrapServerAddress()));
		connect(bootstrapServer.getNegative(Timer.class), master.getPositive(Timer.class));
		connect(bootstrapServer.getPositive(Web.class), jettyWebServer.getNegative(Web.class),
				new WebRequestDestinationFilter(bootConfiguration.getBootstrapServerAddress()
						.getId()));

		connect(monitorServer.getNegative(Network.class), master.getPositive(Network.class),
				new MessageDestinationFilter(monitorConfiguration.getMonitorServerAddress()));
		connect(monitorServer.getNegative(Timer.class), master.getPositive(Timer.class));
		connect(monitorServer.getPositive(Web.class), jettyWebServer.getNegative(Web.class),
				new WebRequestDestinationFilter(monitorConfiguration.getMonitorServerAddress()
						.getId()));

		connect(cyclonSimulator.getNegative(Network.class), master.getPositive(Network.class));
		connect(cyclonSimulator.getNegative(Timer.class), master.getPositive(Timer.class));
		connect(cyclonSimulator.getPositive(Web.class), jettyWebServer.getNegative(Web.class));
		connect(cyclonSimulator.getNegative(CyclonSimulatorPort.class), master
				.getPositive(CyclonSimulatorPort.class));

		connect(master.getNegative(EventuallyPerfectFailureDetector.class), fd
				.getPositive(EventuallyPerfectFailureDetector.class));
	}

}
