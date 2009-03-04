package se.sics.kompics.kdld.main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.kdld.Daemon;
import se.sics.kompics.kdld.main.event.Deploy;
import se.sics.kompics.kdld.main.event.DeployRequest;
import se.sics.kompics.kdld.main.event.DeployResponse;
import se.sics.kompics.kdld.main.event.LaunchResponse;
import se.sics.kompics.launch.Topology;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.mina.MinaNetwork;
import se.sics.kompics.network.mina.MinaNetworkInit;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;


public class ClientRoot extends ComponentDefinition {
	
	static {
		PropertyConfigurator.configureAndWatch("log4j.properties");
	}

	private static final int PORT = 4444;
	private static int id = 0;
	
	private Component time;
	private Component network;
	private Component daemon;

	private static Address self;
	
	private static List<Address> listHosts;
	
	private static final Logger logger = LoggerFactory
	.getLogger(ClientRoot.class);

	private final int ID = 0;
	
	private int numDeployResponses = 0;
	private int numLaunchResponses = 0;
	
	private int lastCommand;
	private String[] commands;
	
	
	private static int selfId;
	private static String commandScript;
	
	private static Topology topology;
	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		
		if (args.length != 2)
		{
			System.out.println("This main is called by the distributed system launcher program.");
			System.out.println("Usage: <prog> id \"commandscript\"");
			System.out.println("Num of args was " + args.length);
			System.exit(-1);
		}
		
		selfId = Integer.parseInt(args[0]);
		commandScript = args[1];
		
		String prop = System.getProperty("topology");
		topology = Topology.load(prop, selfId);
		
		self = topology.getSelfAddress();
		listHosts = new ArrayList<Address>(topology.getNeighbors(self));

		Kompics.createAndStart(ClientRoot.class);
	}

	/**
	 * Instantiates a new assignment0 group0.
	 */
	public ClientRoot() {
		
		// create components
		time = create(JavaTimer.class);
		network = create(MinaNetwork.class);
		daemon = create(Daemon.class);			

		// handle possible faults in the components
		subscribe(handleFault, time.getControl());
		subscribe(handleFault, network.getControl());
		subscribe(handleFault, daemon.getControl());
		
		subscribe(handleDeployResponse, network.getPositive(Network.class));
		subscribe(handleLaunchResponse, network.getPositive(Network.class));	

		subscribe(handleStart, control);

		trigger(new MinaNetworkInit(self), network.getControl());
		
		connect(daemon.getNegative(Network.class), network
				.getPositive(Network.class));
		connect(daemon.getNegative(Timer.class), time
				.getPositive(Timer.class));
	}

	Handler<Fault> handleFault = new Handler<Fault>() {
		public void handle(Fault fault) {
			fault.getFault().printStackTrace(System.err);
		}
	};

	private Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {
			logger.info("Root started.");
			
		}
	};  
	
	
	private Handler<DeployResponse> handleDeployResponse = new Handler<DeployResponse>() {
		public void handle(DeployResponse event) {
			numDeployResponses++;
			
			if (numDeployResponses == listHosts.size())
			{
				 // all responses received, now deployed
				logger.info("All deploy requests completed.");
				doNextCommand();
			}
			
		}
	}; 

	private Handler<LaunchResponse> handleLaunchResponse = new Handler<LaunchResponse>() {
		public void handle(LaunchResponse event) {
			numLaunchResponses++;
			
			if (numLaunchResponses == listHosts.size())
			{
				 // all responses received, now deployed
				logger.info("All Launch requests completed.");
				doNextCommand();
			}
		}
	};

	
	private final void doNextCommand() {
		lastCommand++;

		if (lastCommand > commands.length) {
			return;
		}
		if (lastCommand == commands.length) {
			logger.info("DONE ALL OPERATIONS");
			Thread applicationThread = new Thread("ApplicationThread") {
				public void run() {
					BufferedReader in = new BufferedReader(
							new InputStreamReader(System.in));
					while (true) {
						try {
							String line = in.readLine();
							doCommand(line);
						} catch (Throwable e) {
							e.printStackTrace();
						}
					}
				}
			};
			applicationThread.start();
			return;
		}
		String op = commands[lastCommand];
		doCommand(op);
	}
	
	private void doCommand(String cmd) {
		logger.info("Comand:" + cmd.substring(0, 1));
		if (cmd.startsWith("deploy")) {
			doDeploy(cmd.substring(6));
			// wait for DeployResponses
		} else if (cmd.startsWith("launch")) {
			doLaunch(cmd.substring(6));
			// wait for LaunchResponses
		} else if (cmd.startsWith("X")) {
			doShutdown();
		} else if (cmd.equals("help")) {
			doHelp();
			doNextCommand();
		} else {
			logger.info("Bad command: '{}'. Try 'help'", cmd);
			doNextCommand();
		}
	}
	
	private void doLaunch(String launch)
	{
		numLaunchResponses = 0;
	}
	
	private void doDeploy(String deploy)
	{
		String[] params = deploy.split(",");
		if (params.length != 4)
		{
			logger.warn("Deploy usage: <repoUrl, groupId, artifactId, versionId>");
			return;
		}
		
		numDeployResponses = 0;
		for (Address dest : listHosts)
		{
			DeployRequest d = new DeployRequest(params[0], params[1], 
					params[2], params[3], self, dest);
			trigger(d,network.getPositive(Network.class));
		}
	}
	
	private void doShutdown() {
		System.out.close();
		System.err.close();
		System.exit(0);
	}
	
	private final void doHelp() {
		logger.info("Available commands: deploy<uri>, launch<uri>, help, X");
		logger.info("deploy<repoUri,groupId,artifactId,versionId>: Deploy jar file for artifact.");
		logger.info("launch<repoUri,groupId,artifactId,versionId>: Launch artifact.");		
		logger.info("Sn: sleeps 'n' milliseconds before the next command");
		logger.info("help: shows this help message");
		logger.info("X: terminates this process");
	}
}
