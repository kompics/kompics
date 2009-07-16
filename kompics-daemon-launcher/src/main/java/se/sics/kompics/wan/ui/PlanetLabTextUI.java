package se.sics.kompics.wan.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.util.ConcurrentHashSet;
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
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.config.MasterConfiguration;
import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.master.Master;
import se.sics.kompics.wan.master.MasterInit;
import se.sics.kompics.wan.master.MasterPort;
import se.sics.kompics.wan.master.PrintConnectedDameons;
import se.sics.kompics.wan.master.PrintDaemonsWithLoadedJob;
import se.sics.kompics.wan.master.PrintLoadedJobs;
import se.sics.kompics.wan.master.ShutdownDaemonRequest;
import se.sics.kompics.wan.master.StartJobOnHosts;
import se.sics.kompics.wan.plab.PLabComponent;
import se.sics.kompics.wan.plab.PLabHost;
import se.sics.kompics.wan.plab.PLabPort;
import se.sics.kompics.wan.plab.PlanetLabCredentials;
import se.sics.kompics.wan.plab.events.AddHostsToSliceRequest;
import se.sics.kompics.wan.plab.events.AddHostsToSliceResponse;
import se.sics.kompics.wan.plab.events.GetNodesForSliceResponse;
import se.sics.kompics.wan.plab.events.PLabInit;
import se.sics.kompics.wan.plab.events.UpdateCoMonStats;
import se.sics.kompics.wan.ssh.ExperimentHost;
import se.sics.kompics.wan.ssh.Host;
import se.sics.kompics.wan.ssh.SshComponent;
import se.sics.kompics.wan.ssh.SshPort;
import se.sics.kompics.wan.ssh.events.SshConnectRequest;
import se.sics.kompics.wan.ssh.events.SshConnectResponse;
import se.sics.kompics.wan.ssh.events.SshHeartbeatRequest;
import se.sics.kompics.wan.ssh.events.SshHeartbeatResponse;
import se.sics.kompics.wan.ssh.events.UploadFileRequest;
import se.sics.kompics.wan.util.HostsParser;
import se.sics.kompics.wan.util.HostsParserException;

public class PlanetLabTextUI extends ComponentDefinition {
	public static final int PLAB_CONNECT_TIMEOUT = 30 * 1000;
	
	private Component timer;
	private Component network;
	private Component plab;
	private Component master;
	private Component ssh;

	private PlanetLabCredentials cred;

	private Set<PLabHost> connectedHosts = new ConcurrentHashSet<PLabHost>();
	private Map<Integer, Boolean> mapConnectedHosts = new ConcurrentHashMap<Integer, Boolean>();
	
	private List<PLabHost> availableHosts = new CopyOnWriteArrayList<PLabHost>();

	private static final Logger logger = LoggerFactory.getLogger(PlanetLabTextUI.class);

	private boolean cleanupStarted = false;

	private final HashSet<UUID> outstandingTimeouts = new HashSet<UUID>();
	
	public static class CleanupConnections extends Timeout {

		public CleanupConnections(ScheduleTimeout request) {
			super(request);
		}

	}
	
	public class SshConnectTimeout extends Timeout {

		private final int numRetries;
		
		private final PLabHost host;
		
		public SshConnectTimeout(ScheduleTimeout request, PLabHost host, int numRetries) {
			super(request);
			this.host = host;
			this.numRetries = numRetries;
		}
		
		public PLabHost getHost() {
			return host;
		}
		
		public int getNumRetries() {
			return numRetries;
		}
	}
	

	public PlanetLabTextUI() {

		// create components
		timer = create(JavaTimer.class);
		network = create(MinaNetwork.class);
		plab = create(PLabComponent.class);
		master = create(Master.class);
		ssh = create(SshComponent.class);

		getCredentials();

		PLabInit pInit = new PLabInit(cred);
		trigger(pInit, plab.getControl());

		MasterInit mInit = new MasterInit(PlanetLabConfiguration.getMasterAddress(),
				PlanetLabConfiguration.getBootConfiguration(), PlanetLabConfiguration
						.getMonitorConfiguration());

		trigger(mInit, master.getControl());

		trigger(new MinaNetworkInit(MasterConfiguration.getMasterAddress()), network.getControl());

		InetAddress ip = PlanetLabConfiguration.getIp();
		int rpcPort = PlanetLabConfiguration.getXmlRpcPort();
		int requestTimeout = PlanetLabConfiguration.getXmlRpcTimeout();
		int maxThreads = PlanetLabConfiguration.getXmlRpcMaxThreads();
		String homepage = PlanetLabConfiguration.getXmlRpcHomepage();

		logger.info("Master listening on: {}", MasterConfiguration.getMasterAddress().toString());

		connectToNetAndTimer(plab);
		connectToNetAndTimer(master);
		connect(ssh.getNegative(Timer.class), timer.getPositive(Timer.class));

		// handle possible faults in the components
		subscribe(handleFault, timer.getControl());
		subscribe(handleFault, network.getControl());
		subscribe(handleFault, plab.getControl());

		subscribe(handleStart, control);

		subscribe(handleSshConnectResponse, ssh.getPositive(SshPort.class));
		subscribe(handleSshHeartbeatResponse, ssh.getPositive(SshPort.class));
		
		subscribe(handleGetNodesForSliceResponse, plab.getPositive(PLabPort.class));
		subscribe(handleAddHostsToSliceResponse, plab.getPositive(PLabPort.class));
		
		subscribe(handleCleanupConnections, timer.getPositive(Timer.class));
	}

	private void connectToNetAndTimer(Component c) {
		connect(c.getNegative(Network.class), network.getPositive(Network.class));
		connect(c.getNegative(Timer.class), timer.getPositive(Timer.class));
	}

	private void getBootstates() {
		trigger(new UpdateCoMonStats(), plab.getPositive(PLabPort.class));

	}

	private void getCredentials() {
		Scanner scanner = new Scanner(System.in);

		String username = PlanetLabConfiguration.getUsername();
		if (username.compareTo("") == 0) {
			System.out.print("\tEnter planetlab username: ");
			username = scanner.next();
		}

		String password = PlanetLabConfiguration.getPassword();
		if (password.compareTo("") == 0) {
			System.out.print("\tEnter planetlab password: ");
			password = scanner.next();
		}

		String slice = PlanetLabConfiguration.getSlice();
		if (slice.compareTo("") == 0) {
			System.out.print("\tEnter name of slice: ");
			slice = scanner.next();
		}
		String role = PlanetLabConfiguration.getRole();
		if (role.compareTo("") == 0) {
			System.out.print("\tEnter the planetlab role (user, tech, admin): ");
			role = scanner.next();
		}

		String keyPath = PlanetLabConfiguration.getPrivateKeyFile();
		if (keyPath.compareTo("") == 0) {
			System.out.print("\tEnter the full pathname for the private key file: ");
			keyPath = scanner.next();
		}

		String keyFilePassword = PlanetLabConfiguration.getPrivateKeyFilePassword();
		if (keyFilePassword.compareTo("") == 0) {
			// System.out.print("\tEnter the password for the private key file: ");
			// keyFilePassword = scanner.next();
		}

		cred = new PlanetLabCredentials("kost@sics.se", password, "sics_grid4all",
				"/home/jdowling/.ssh/id_rsa", "");
	}

	public Handler<GetNodesForSliceResponse> handleGetNodesForSliceResponse 
	= new Handler<GetNodesForSliceResponse>() {
		public void handle(GetNodesForSliceResponse event) {
			
			Set<PLabHost> hosts =event.getHosts(); 
			connectedHosts.addAll(hosts);

			logger.info("Added {} hosts from PlanetLabComponent", hosts.size());
		}
	};
	
	public Handler<SshConnectResponse> handleSshConnectResponse = new Handler<SshConnectResponse>() {
		public void handle(SshConnectResponse event) {
			
			if (outstandingTimeouts.contains(event.getRequestId())) {
				CancelTimeout ct = new CancelTimeout(event.getRequestId());
				trigger(ct, timer.getPositive(Timer.class));
				outstandingTimeouts.remove(event.getRequestId());
			} else {
				// request was retried. we ignore this first slow response.
				// (to avoid double response;TODO add a local BOOTSTRAPPED flag
				// per overlay)
				return;
			}
			int sessionId = event.getSessionId();

			Host host = event.getHostname();
			host.setSessionId(sessionId);
			PLabHost plHost = new PLabHost(host);

			connectedHosts.add(plHost);

			if (cleanupStarted == false) {
				ScheduleTimeout st = new ScheduleTimeout(10 * 1000);
				st.setTimeoutEvent(new CleanupConnections(st));
				trigger(st, timer.getPositive(Timer.class));
			}
		}
	};

	public Handler<CleanupConnections> handleCleanupConnections = new Handler<CleanupConnections>() {
		public void handle(CleanupConnections event) 
		{

			// XXX remove connections that haven't sent a reply for N heartbeats			
			for (PLabHost host : connectedHosts)
			{
				int sId = host.getSessionId();
				Boolean recvdHB = mapConnectedHosts.get(sId);
				if (recvdHB == null)
				{
					host.incHearbeatTimeout();
				}
				else {
					if (recvdHB == true) {
						host.zeroHearbeatTimeout();
					}
				}
				if (host.getHeartbeatTimeout() > 3) {
					connectedHosts.remove(host);
				}
				else {
					// ping the remaining open ssh connections 
					trigger(new SshHeartbeatRequest(sId), ssh.getPositive(SshPort.class));
				}
			}
			mapConnectedHosts.clear();
			
			
		}
	};
	
	public Handler<SshHeartbeatResponse> handleSshHeartbeatResponse = new Handler<SshHeartbeatResponse>() {
		public void handle(SshHeartbeatResponse event) 
		{
			mapConnectedHosts.put(event.getSessionId(), event.isStatus());
		}
	};
	
	public Handler<AddHostsToSliceResponse> handleAddHostsToSliceResponse = 
		new Handler<AddHostsToSliceResponse>() {
		public void handle(AddHostsToSliceResponse event) 
		{
			System.out.println("Result of adding hosts to slice was: " + event.getHostStatus());
		}
	};
	
	
	
	public Handler<SshConnectTimeout> handleSshConnectTimeout = new Handler<SshConnectTimeout>() {
		public void handle(SshConnectTimeout event) {
			// XXX Check if the timer was cancelled
			if (outstandingTimeouts.contains(event.getTimeoutId()) == false) {
				return;
			}
			
			connectToHost(event.getHost(), event.getNumRetries());
		}
	};
	
	

	private void connectToHost(PLabHost host, int retriedNumber)
	{
		if (retriedNumber >= Configuration.DEFAULT_RETRY_COUNT) {
			logger.warn("Exceed max number of retries for host: {}", host.getHostname());
			return;
		}
		
		
		try {
			PlanetLabConfiguration.getNetworkIntensiveTicket();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		
		ScheduleTimeout st = new ScheduleTimeout(PLAB_CONNECT_TIMEOUT);
		SshConnectTimeout connectTimeout = new SshConnectTimeout(st, host, retriedNumber+1);
		st.setTimeoutEvent(connectTimeout);

		UUID timerId = connectTimeout.getTimeoutId();
		outstandingTimeouts.add(timerId);

		trigger(new SshConnectRequest(cred, timerId, new ExperimentHost(host)), 
				ssh.getPositive(SshPort.class));

		trigger(st, timer.getPositive(Timer.class));				

		PlanetLabConfiguration.releaseNetworkIntensiveTicket();
	}

	private class UserInput extends Thread {
		private AtomicBoolean finished = new AtomicBoolean(false);
		private final Scanner scanner;

		public UserInput() {
			scanner = new Scanner(System.in);
		}

		@Override
		public void run() {

			while (finished.get() == false) {
				switch (selectMenuOption()) {
				case 1:
					enterMaster();
					break;
				case 2:
					enterPlanetLab();
					break;
				case 0:
					System.exit(0);
					return;
				default:
					break;

				}
			}

		}

		private void enterPlanetLab() {
			while (true) {
				TreeSet<Address> hosts = MasterConfiguration.getHosts();
				switch (selectPlanetlabOption()) {
				case 0:
					return;
				case 1:
					getCredentials();
					break;
				case 4:
					connectToAllHosts();
					break;
				case 5:
					addHostsToSlice();
					break;
				case 6:
					getBootstates();
					break;
				default:
					break;
				}
			}
		}

		private void enterMaster() {
			while (true) {
				TreeSet<Address> hosts = MasterConfiguration.getHosts();
				switch (selectMasterOption()) {
				case 1:
					trigger(new PrintConnectedDameons(), master.getNegative(MasterPort.class));
					break;
				case 2:
					trigger(new PrintDaemonsWithLoadedJob(getJob()), master
							.getNegative(MasterPort.class));
					break;
				case 3:
					System.out.print("\tEnter daemon-id: ");
					int daemonId = scanner.nextInt();
					trigger(new PrintLoadedJobs(daemonId), master.getNegative(MasterPort.class));
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
					boolean hideOutput = (hideMavenOutput.compareToIgnoreCase("y") == 0) ? true
							: false;

					// System.out.print("\tEnter any optional args (return for none): ");
					// String allArgs = scanner.next();
					// String[] args = allArgs.split(" ");
					String[] args = {};
					// trigger(new InstallJobOnHosts(groupId, artifactId,
					// version, mainClass, Arrays
					// .asList(args), hideOutput, hosts), master);
					break;
				case 6:
					int jobId = getJob();
					// trigger(new StopJobOnHosts(jobId), master);
					break;
				case 7:
					// copy daemon jar to all hosts

					copyDaemonToHosts();

					break;
				case 8:
					// connect to all hosts

					break;
				case 9:
					trigger(new StartJobOnHosts(getJob(), getNumPeers()), master
							.getNegative(MasterPort.class));
					break;
				case 10:
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
					if (hosts == null) {
						while (selectHostsFile() == false)
							;
					}

					break;
				case 11:
					trigger(new ShutdownDaemonRequest(), master.getNegative(MasterPort.class));
					break;
				case 0:
					return;
				default:
					System.out.println();
					System.out.println("Invalid choice.");
					System.out.println();
					break;
				}
				System.out.println();
			}
		}

		private void copyDaemonToHosts() {
			validateListHosts();

			for (PLabHost host : connectedHosts) {
				String daemonJarFilename = getDaemonJarFile();
				File file = new File(daemonJarFilename);
				int pos = daemonJarFilename.lastIndexOf('/');
				int sessionId = host.getSessionId();

				UploadFileRequest uploadJar = new UploadFileRequest(sessionId, file, "~/kompics/"
						+ daemonJarFilename.substring(pos), true, 10 * 1000.0, true);

				trigger(uploadJar, ssh.getNegative(SshPort.class));
			}
		}

		private void connectToAllHosts() {
			validateListHosts();

			for (PLabHost h : connectedHosts) {

				connectToHost(h, 0);
			}
			
		}
		
		private void addHostsToSlice()
		{
			System.out.println();
			System.out.println("Enter the name of the hosts to add to the slice.");
			System.out.println("Enter 'x' to finish entering hosts");
			String entered = scanner.next();
			Set<String> hosts = new HashSet<String>();
			while (entered.compareToIgnoreCase("x") != 0) {
				boolean found = false;
				for (PLabHost h : availableHosts) {
					if (h.getHostname().compareToIgnoreCase(entered) == 0) {
						found = true;
					}
				}
				if (found == true) {
					hosts.add(entered);
				}
				else {
					System.out.println("You haven't downloaded this host. Invalid hostname.");
				}
				entered = scanner.next();
			}
			
			AddHostsToSliceRequest req = new AddHostsToSliceRequest(hosts);
			trigger(req, plab.getPositive(PLabPort.class));
			
		}
		

		private boolean selectHostsFile() {
			boolean succeed = true;
			System.out.println();
			System.out.println("Enter the full pathname of the file containing a list"
					+ "of comma-separated hosts in the format host[:port[:id]] ");
			String filename = scanner.next();
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

		private String getDaemonJarFile() {
			System.out.print("\tEnter full pathname for daemon jar file: ");
			return scanner.next();
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

		private int selectPlanetlabOption() {
			System.out.println();
			System.out.println("Enter a number to select an option from below:");
			System.out.println("\t1) change planet-lab credentials.");
			System.out.println("\t2) add a host to a slice.");
			System.out.println("\t3) list hosts in slice.");
			System.out.println("\t4) list planetlab sites using CoMon.");
			System.out.println("\t5) add hosts to a slice.");
			System.out.println("\t6) GET BOOT STATES.");
			System.out.println("\t7) scp (copy) daemon jar file to hosts.");
			System.out.println("\t8) .");
			System.out.println("\t9) .");
			System.out.println("\t0) back");
			System.out.print("Enter your choice: ");
			return scanner.nextInt();
		}

		private int selectMenuOption() {
			System.out.println();
			System.out.println("Enter a number to select an option from below:");
			System.out.println("\t1) enter master.");
			System.out.println("\t2) enter planetlab.");
			System.out.println("\t0) exit program");
			System.out.print("Enter your choice: ");
			return scanner.nextInt();
		}

		private int selectMasterOption() {
			System.out.println();
			System.out.println("Enter a number to select an option from below:");
			System.out.println("\t0) back");
			// System.out.println("\t1) list connected daemons.");
			// System.out
			// .println("\t2) specify a job, and list all daemons that have loaded it.");
			// System.out
			// .println("\t3) specify a daemon, and list all its loaded jobs.");
			// System.out.println("\t4) load a job to all hosts.");
			// System.out.println("\t5) load a job to selected hosts.");
			// System.out.println("\t6) stop a job on all hosts.");
			System.out.println("\t7) scp daemon jar file to hosts.");
			System.out.println("\t8) connect to all hosts.");

			// System.out
			// .println("\t9) start a job on all hosts that have loaded the job.");
			System.out.println("\t11) shutdown all hosts.");
			System.out.print("Enter your choice: ");
			return scanner.nextInt();
		}

		public void exit() {
			this.finished.set(true);
		}

		private void validateListHosts() {
			if (connectedHosts.size() == 0) {
				logger.warn("Warning: there are no hosts available to perform your operation with.");
			}
		}

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
