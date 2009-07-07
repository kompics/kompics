package se.sics.kompics.wan.ui;


import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.mina.MinaNetwork;
import se.sics.kompics.network.mina.MinaNetworkInit;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.kompics.wan.config.MasterConfiguration;
import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.master.InstallJobOnHosts;
import se.sics.kompics.wan.master.Master;
import se.sics.kompics.wan.master.MasterInit;
import se.sics.kompics.wan.master.PrintConnectedDameons;
import se.sics.kompics.wan.master.PrintDaemonsWithLoadedJob;
import se.sics.kompics.wan.master.PrintLoadedJobs;
import se.sics.kompics.wan.master.ShutdownDaemonRequest;
import se.sics.kompics.wan.master.StartJobOnHosts;
import se.sics.kompics.wan.master.StopJobOnHosts;
import se.sics.kompics.wan.master.plab.PLabComponent;
import se.sics.kompics.wan.master.plab.PlanetLabCredentials;
import se.sics.kompics.wan.master.plab.plc.events.PlanetLabInit;
import se.sics.kompics.wan.master.ssh.SshComponent;
import se.sics.kompics.wan.util.HostsParser;
import se.sics.kompics.wan.util.HostsParserException;


public class PlanetLabTextUI extends ComponentDefinition {

	private Component time;
	private Component network;
	private Component plab;
	private Component master;
	private Component ssh;

	private static final Logger logger = LoggerFactory.getLogger(PlanetLabTextUI.class);

	
	private class UserInput extends Thread
	{
		private AtomicBoolean finished = new AtomicBoolean(false);
		private final Scanner scanner;

		public UserInput() {
			scanner = new Scanner(System.in);
		}
		
		@Override
		public void run() {

			while (finished.get() == false)
			{
			
				TreeSet<Address> hosts = MasterConfiguration.getHosts();
				switch (selectOption()) {
				case 1:
//					trigger(new PrintConnectedDameons(), master);
					break;
				case 2:
//					trigger(new PrintDaemonsWithLoadedJob(getJob()), master);
					break;
				case 3:
					System.out.print("\tEnter daemon-id: ");
					int daemonId = scanner.nextInt();
//					trigger(new PrintLoadedJobs(daemonId), master);
					break;
				case 5: // XXX
					hosts = getHosts();
					// deliberate skip of 'break' here.
				case 4:
					System.out.print("\tEnter groupId: ");
					String groupId = scanner.next();
					System.out.print("\tEnter artifactId: ");
					String artifactId = scanner.next();
					System.out.print("\tEnter version: ");
					String version = scanner.next();
					System.out.print("\tEnter mainClass: ");
					String mainClass = scanner.next();
					System.out.print("\tHide Maven output (y/n): ");
					String hideMavenOutput = scanner.next();
					boolean hideOutput = (hideMavenOutput.compareToIgnoreCase("y") == 0) ? true : false;

					// System.out.print("\tEnter any optional args (return for none): ");
					// String allArgs = scanner.next();
					// String[] args = allArgs.split(" ");
					String[] args = {};
//					trigger(new InstallJobOnHosts(groupId, artifactId, version, mainClass, Arrays
//							.asList(args), hideOutput, hosts), master);
					break;
				case 6:
					int jobId = getJob();
//					trigger(new StopJobOnHosts(jobId), master);
					break;
				case 7:
					int sshAuthOpt = 0;
					do {
						sshAuthOpt = selectSshAuthMethod();
					} while (sshAuthOpt < 1 || sshAuthOpt > 2);
					switch (sshAuthOpt) {
					case 1: 
						break;
					case 2:
						break;
					}
					
					hosts = MasterConfiguration.getHosts(); 
					if (hosts == null)
					{
						while (selectHostsFile() == false) ;
					}
					
					break;
				case 8:
//					trigger(new ShutdownDaemonRequest(), master);
					break;
				case 9:
//					trigger(new StartJobOnHosts(getJob(), getNumPeers()), master);
					break;
				case 0:
					System.out.println("\tGoodbye.");
					System.out.println("\tExiting.....");
					System.out.println();
//					System.exit(0);
					exit();
					break;
				default:
					System.out.println();
					System.out.println("Invalid choice.");
					System.out.println();
					break;
				}
				System.out.println();
			}			
		}
		
		private boolean selectHostsFile()
		{
			boolean succeed = true;
			System.out.println();
			System.out.println("Enter the full pathname of the file containing a list" +
					"of comma-separated hosts in the format host[:port[:id]] ");
			String filename =  scanner.next();
			try {
				TreeSet<Address> hosts = HostsParser.parseHostsFile(filename);
			} catch (FileNotFoundException e) {
				System.out.println("File not found: " + e.getMessage());
				return false;
			} catch (HostsParserException e) {
				System.out.println("Hosts file not formatted correctly: " + e.getMessage());
				return false;
			}		
			
			return succeed;
		}
		
		private int getJob() {
			System.out.print("\tEnter job id: ");
			return scanner.nextInt();
		}

		private int getNumPeers() {
			System.out.print("\tEnter the number of peers to start at each host: ");
			return scanner.nextInt();
		}

		
		private TreeSet<Address> getHosts() {
			int first, last;
			System.out.println("Enter the start of the range of hosts to use:");
			first = scanner.nextInt();
			System.out.println("Enter the end of the range of hosts to use:");
			last = scanner.nextInt();
			return MasterConfiguration.getHosts(first, last);
		}
		
		private int selectSshAuthMethod() {
			System.out.println();
			System.out.println("Enter a number to select an option from below:");
			System.out.println("\t1) username/password.");
			System.out.println("\t2) public-key authentication.");
			System.out.print("Enter your choice: ");
			return scanner.nextInt();
		}

		private int selectOption() {
			System.out.println();
			System.out.println("Enter a number to select an option from below:");
			System.out.println("\t1) list connected daemons.");
			System.out.println("\t2) specify a job, and list all daemons that have loaded it.");
			System.out.println("\t3) specify a daemon, and list all its loaded jobs.");
			System.out.println("\t4) load a job to all hosts.");
			System.out.println("\t5) load a job to selected hosts.");
			System.out.println("\t6) stop a job on all hosts.");
			System.out.println("\t7) scp (copy) daemon jar file to hosts.");
			System.out.println("\t8) shutdown all hosts.");
			System.out.println("\t9) start a job on all hosts that have loaded the job.");
			System.out.println("\t0) exit program");
			System.out.print("Enter your choice: ");
			return scanner.nextInt();
		}
		
		public void exit() {
			this.finished.set(true);
		}
	}
	

	/**
	 * Instantiates a new assignment0 group0.
	 */
	public PlanetLabTextUI() {

		// create components
		time = create(JavaTimer.class);
		network = create(MinaNetwork.class);
		plab = create(PLabComponent.class);
		master = create(Master.class);
		ssh = create(SshComponent.class);

		// handle possible faults in the components
		subscribe(handleFault, time.getControl());
		subscribe(handleFault, network.getControl());
		subscribe(handleFault, plab.getControl());
		
		String username = PlanetLabConfiguration.getUsername();
		String password = PlanetLabConfiguration.getPassword();
		String slice = PlanetLabConfiguration.getSlice();
		String role = PlanetLabConfiguration.getRole(); 
		String keyPath = PlanetLabConfiguration.getPrivateKeyFile();
		String keyFilePassword = PlanetLabConfiguration.getPrivateKeyFilePassword();
		
		PlanetLabCredentials cred = new PlanetLabCredentials(username, password, slice,
				role, keyPath, keyFilePassword);
		
		PlanetLabInit pInit = new PlanetLabInit( cred,
				PlanetLabConfiguration.getMasterAddress(),
				PlanetLabConfiguration.getBootConfiguration(), PlanetLabConfiguration
						.getMonitorConfiguration());

		trigger(pInit, plab.getControl());

		MasterInit mInit = new MasterInit(PlanetLabConfiguration.getMasterAddress(),
				PlanetLabConfiguration.getBootConfiguration(), PlanetLabConfiguration
						.getMonitorConfiguration());
		
		trigger(mInit, master.getControl());

		
		
		trigger(new MinaNetworkInit(MasterConfiguration.getMasterAddress()), network.getControl());

		logger.info("Master listening on: {}", MasterConfiguration.getMasterAddress().toString());

		connect(plab.getNegative(Network.class), network.getPositive(Network.class));
		connect(plab.getNegative(Timer.class), time.getPositive(Timer.class));

		connect(ssh.getNegative(Network.class), ssh.getPositive(Network.class));
		connect(ssh.getNegative(Timer.class), ssh.getPositive(Timer.class));

		connect(master.getNegative(Network.class), master.getPositive(Network.class));
		connect(master.getNegative(Timer.class), master.getPositive(Timer.class));
		
		trigger(new Start(), control);
	}

	Handler<Fault> handleFault = new Handler<Fault>() {
		public void handle(Fault fault) {
			fault.getFault().printStackTrace(System.err);
		}
	};
	
	Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {
			
			UserInput ui = new UserInput();
			ui.start();
			
		}
	};

}
