package se.sics.kompics.wan.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Start;
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
import se.sics.kompics.wan.master.events.GetConnectedDameonsRequest;
import se.sics.kompics.wan.master.events.GetDaemonsWithLoadedJobRequest;
import se.sics.kompics.wan.master.events.GetLoadedJobsForDaemonRequest;
import se.sics.kompics.wan.master.events.ShutdownDaemonRequest;
import se.sics.kompics.wan.master.events.StartJobOnHostsRequest;
import se.sics.kompics.wan.plab.CoMonStats;
import se.sics.kompics.wan.plab.PLabComponent;
import se.sics.kompics.wan.plab.PLabHost;
import se.sics.kompics.wan.plab.PLabPort;
import se.sics.kompics.wan.plab.PlanetLabCredentials;
import se.sics.kompics.wan.plab.events.AddHostsToSliceRequest;
import se.sics.kompics.wan.plab.events.AddHostsToSliceResponse;
import se.sics.kompics.wan.plab.events.GetHostsInSliceRequest;
import se.sics.kompics.wan.plab.events.GetHostsInSliceResponse;
import se.sics.kompics.wan.plab.events.GetHostsNotInSliceRequest;
import se.sics.kompics.wan.plab.events.GetHostsNotInSliceResponse;
import se.sics.kompics.wan.plab.events.PLabInit;
import se.sics.kompics.wan.plab.events.RankHostsUsingCoMonRequest;
import se.sics.kompics.wan.plab.events.RankHostsUsingCoMonResponse;
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
import se.sics.kompics.wan.ssh.events.UploadFileResponse;
import se.sics.kompics.wan.ssh.scp.DownloadUploadMgr;
import se.sics.kompics.wan.ssh.scp.DownloadUploadPort;
import se.sics.kompics.wan.ssh.scp.ScpComponent;
import se.sics.kompics.wan.ssh.scp.ScpPort;
import se.sics.kompics.wan.util.HostsParser;
import se.sics.kompics.wan.util.HostsParserException;

public class PlanetLabTextUI extends ComponentDefinition {
	public static final int PLAB_CONNECT_TIMEOUT = 30 * 1000;

	private Component timer;
	private Component network;
	private Component plab;
	private Component master;
	private Component ssh;
	private Component downUp;
	private Component scp;

	private PlanetLabCredentials cred;

	private Set<PLabHost> connectedHosts = new ConcurrentHashSet<PLabHost>();
	private Map<Integer, Boolean> mapConnectedHosts = new ConcurrentHashMap<Integer, Boolean>();
	private Set<Host> availableHosts = new HashSet<Host>();

	private static final Logger logger = LoggerFactory
			.getLogger(PlanetLabTextUI.class);

	private boolean cleanupStarted = false;

	private final HashSet<UUID> outstandingTimeouts = new HashSet<UUID>();

	private UserInput ui = null;

	public static class CleanupConnections extends Timeout {

		public CleanupConnections(ScheduleTimeout request) {
			super(request);
		}

	}

	public class SshConnectTimeout extends Timeout {

		private final int numRetries;

		private final String host;

		public SshConnectTimeout(ScheduleTimeout request, String host,
				int numRetries) {
			super(request);
			this.host = host;
			this.numRetries = numRetries;
		}

		public String getHost() {
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
		scp = create(ScpComponent.class);
		downUp = create(DownloadUploadMgr.class);
		// handle possible faults in the components
		subscribe(handleFault, timer.getControl());
		subscribe(handleFault, network.getControl());
		subscribe(handleFault, plab.getControl());
		subscribe(handleFault, master.getControl());
		subscribe(handleFault, ssh.getControl());
		subscribe(handleFault, scp.getControl());
		subscribe(handleFault, downUp.getControl());

		getCredentials();

		logger.info("Master listening on: {}", MasterConfiguration
				.getMasterAddress().toString());

		// connectToNetAndTimer(plab);
		connectToNetAndTimer(master);
		connect(ssh.getNegative(Timer.class), timer.getPositive(Timer.class));

		// Connect Ssh -> DownloadUploadMgr -> Scp
		connect(ssh.getNegative(DownloadUploadPort.class), downUp
				.getPositive(DownloadUploadPort.class));
		connect(downUp.getNegative(ScpPort.class), scp
				.getPositive(ScpPort.class));

		subscribe(handleStart, control);

		subscribe(handleSshConnectResponse, ssh.getPositive(SshPort.class));
		subscribe(handleSshHeartbeatResponse, ssh.getPositive(SshPort.class));
		subscribe(handleUploadFileResponse, ssh.getPositive(SshPort.class));

		subscribe(handleGetNodesInSliceResponse, plab
				.getPositive(PLabPort.class));
		subscribe(handleGetNodesNotInSliceResponse, plab
				.getPositive(PLabPort.class));
		subscribe(handleAddHostsToSliceResponse, plab
				.getPositive(PLabPort.class));
		subscribe(handleRankHostsUsingCoMonResponse, plab
				.getPositive(PLabPort.class));

		subscribe(handleCleanupConnections, timer.getPositive(Timer.class));

		PLabInit pInit = new PLabInit(cred,PlanetLabConfiguration.getPlcApiAddress());
		trigger(pInit, plab.getControl());

		MasterInit mInit = new MasterInit(PlanetLabConfiguration
				.getMasterAddress(), PlanetLabConfiguration
				.getBootConfiguration());
//                , PlanetLabConfiguration.getMonitorConfiguration()

		trigger(mInit, master.getControl());

		trigger(new MinaNetworkInit(MasterConfiguration.getMasterAddress()),
				network.getControl());

		availableHosts = PlanetLabConfiguration.getHosts();

	}

	private void connectToNetAndTimer(Component c) {
		connect(c.getNegative(Network.class), network
				.getPositive(Network.class));
		connect(c.getNegative(Timer.class), timer.getPositive(Timer.class));
	}

	private void updateCoMonStats() {
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
			System.out
					.print("\tEnter the planetlab role (user, tech, admin): ");
			role = scanner.next();
		}

		String keyPath = PlanetLabConfiguration.getPrivateKeyFile();
		if (keyPath.compareTo("") == 0) {
			System.out
					.print("\tEnter the full pathname for the private key file: ");
			keyPath = scanner.next();
		}

		String keyFilePassword = PlanetLabConfiguration
				.getPrivateKeyFilePassword();
		if (keyFilePassword.compareTo("") == 0) {
			// System.out.print("\tEnter the password for the private key file: ");
			// keyFilePassword = scanner.next();
		}

		cred = new PlanetLabCredentials("kost@sics.se", password,
				"sics_grid4all", "/home/jdowling/.ssh/id_rsa", "");
	}

	public Handler<GetHostsInSliceResponse> handleGetNodesInSliceResponse = new Handler<GetHostsInSliceResponse>() {
		public void handle(GetHostsInSliceResponse event) {

			Set<PLabHost> hosts = event.getHosts();
			availableHosts.addAll(hosts);

			for (PLabHost h : hosts) {
				System.out.print(h.getHostname() + ", ");
			}
			System.out.println();

			logger.info("Number of hosts registered for this slice: {}",
					availableHosts.size());

			ui.proceed();

		}
	};

	public Handler<GetHostsNotInSliceResponse> handleGetNodesNotInSliceResponse = new Handler<GetHostsNotInSliceResponse>() {
		public void handle(GetHostsNotInSliceResponse event) {

			Set<PLabHost> hosts = event.getHosts();

			for (PLabHost h : hosts) {
				System.out.print(h.getHostname() + ", ");
			}
			System.out.println();

			logger.info("Number of hosts not registered for this slice: {}",
					hosts.size());

			ui.proceed();

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
				// (to avoid double response
				// TODO add a local BOOTSTRAPPED flag
				// per overlay)
				return;
			}
			int sessionId = event.getSessionId();

			Host host = event.getHost();
			host.setSessionId(sessionId);
			PLabHost plHost = new PLabHost(host);

			connectedHosts.add(plHost);

			System.out.println("Ssh connection established to "
					+ host.getHostname() + "(" + host.getSessionId() + ")");

			// XXX uncomment this
			// if (cleanupStarted == false) {
			// ScheduleTimeout st = new ScheduleTimeout(10 * 1000);
			// st.setTimeoutEvent(new CleanupConnections(st));
			// trigger(st, timer.getPositive(Timer.class));
			// }

		}
	};

	public Handler<CleanupConnections> handleCleanupConnections = new Handler<CleanupConnections>() {
		public void handle(CleanupConnections event) {

			// XXX remove connections that haven't sent a reply for N heartbeats
			for (PLabHost host : connectedHosts) {
				int sId = host.getSessionId();
				Boolean recvdHB = mapConnectedHosts.get(sId);
				if (recvdHB == null) {
					host.incHearbeatTimeout();
				} else {
					if (recvdHB == true) {
						host.zeroHearbeatTimeout();
					}
				}
				if (host.getHeartbeatTimeout() > 3) {
					connectedHosts.remove(host);
				} else {
					// ping the remaining open ssh connections
					trigger(new SshHeartbeatRequest(sId), ssh
							.getPositive(SshPort.class));
				}
			}
			mapConnectedHosts.clear();

		}
	};

	public Handler<SshHeartbeatResponse> handleSshHeartbeatResponse = new Handler<SshHeartbeatResponse>() {
		public void handle(SshHeartbeatResponse event) {
			mapConnectedHosts.put(event.getSessionId(), event.isStatus());
		}
	};

	public Handler<AddHostsToSliceResponse> handleAddHostsToSliceResponse = new Handler<AddHostsToSliceResponse>() {
		public void handle(AddHostsToSliceResponse event) {
			System.out.println("Result of adding hosts to slice was: "
					+ event.getHostStatus());
			ui.proceed();
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

	public Handler<UploadFileResponse> handleUploadFileResponse = new Handler<UploadFileResponse>() {
		public void handle(UploadFileResponse event) {

			System.out.println("Received upload response : "
					+ event.getFile().getAbsolutePath());

			ui.proceed();

		}
	};

	public Handler<RankHostsUsingCoMonResponse> handleRankHostsUsingCoMonResponse = new Handler<RankHostsUsingCoMonResponse>() {
		public void handle(RankHostsUsingCoMonResponse event) {

			System.out.println("Here is the list of Hosts ranked after "
					+ event.getRanking() + " :");
			for (PLabHost h : event.getHosts()) {
				System.out.print("[" + h.getHostname());

				for (String stat : event.getRanking()) {
					double val;
					if (h.getComMonStat() == null) {
						val = Double.NaN;
					} else {
						val = h.getComMonStat().getStat(stat);
					}

					System.out.print(", " + stat + "=" + val);
				}
				System.out.print("] ");
			}
			System.out.println();
			ui.proceed();
		}
	};

	private void connectToHost(String host, int retriedNumber) {
		if (retriedNumber >= Configuration.DEFAULT_RETRY_COUNT) {
			logger.warn("Exceed max number of retries for host: {}", host);
			return;
		}

		try {
			PlanetLabConfiguration.acquireNetworkIntensiveTicket();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		ScheduleTimeout st = new ScheduleTimeout(PLAB_CONNECT_TIMEOUT);
		SshConnectTimeout connectTimeout = new SshConnectTimeout(st, host,
				retriedNumber + 1);
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
		private AtomicBoolean proceed = new AtomicBoolean(false);

		public UserInput() {
			scanner = new Scanner(System.in);
		}

		public void proceed() {
			synchronized (this) {
				proceed.set(true);
				notify();
			}
		}

		@Override
		public void run() {

			trigger(new GetHostsInSliceRequest(cred, false), plab
					.getPositive(PLabPort.class));

			synchronized (this) {
				while (proceed.get() == false) {
					try {
						wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

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

				proceed.set(false);

				try {
					switch (selectPlanetlabOption()) {
					case 0:
						return;
					case 1:
						getCredentials();
						proceed();
						break;
					case 2:
						listHostsRegisteredForThisSlice();
						break;
					case 3:
						listHostsNotRegisteredForThisSlice();
						break;
					case 4:
						addHostsToSlice();
						break;
					case 5:
						rankHostsUsingCoMon();
						break;
					case 6:
						connectToHosts();
						break;
					case 7:
						updateCoMonStats();
						break;
					case 8:
						copyDaemonToHosts();
						break;
					default:
						break;
					}
				} catch (java.util.InputMismatchException e) {
					System.out
							.println("Invalid choice. You must enter a valid number.");
					proceed.set(true);
				}

				synchronized (this) {
					while (proceed.get() == false) {
						try {
							wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

			}
		}

		private void enterMaster() {
			while (true) {
				Set<Host> hosts = MasterConfiguration.getHosts();
				switch (selectMasterOption()) {
				case 1:
					trigger(new GetConnectedDameonsRequest(), master
							.getNegative(MasterPort.class));
					break;
				case 2:
					trigger(new GetDaemonsWithLoadedJobRequest(getJob()), master
							.getNegative(MasterPort.class));
					break;
				case 3:
					System.out.print("\tEnter daemon-id: ");
					int daemonId = scanner.nextInt();
					trigger(new GetLoadedJobsForDaemonRequest(daemonId), master
							.getNegative(MasterPort.class));
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
					boolean hideOutput = (hideMavenOutput
							.compareToIgnoreCase("y") == 0) ? true : false;

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
					trigger(new StartJobOnHostsRequest(getJob(), getNumPeers()),
							master.getNegative(MasterPort.class));
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
					trigger(new ShutdownDaemonRequest(), master
							.getNegative(MasterPort.class));
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

		private boolean yesNoQuestion(String question) {
			boolean forceDownload = false;
			boolean success = false;

			while (!success) {
				System.out.println(question);
				String ans = scanner.next();
				if (ans.compareToIgnoreCase("y") == 0
						|| ans.compareToIgnoreCase("yes") == 0) {
					forceDownload = true;
					success = true;
				} else if (ans.compareToIgnoreCase("n") == 0
						|| ans.compareToIgnoreCase("no") == 0) {
					forceDownload = false;
					success = true;
				} else {
					System.err
							.println("Error in input: please enter a 'y' (yes) or 'n' (no) as a valid answer!");
				}
			}
			return forceDownload;
		}

		private void listHostsNotRegisteredForThisSlice() {
			boolean forceDownload = yesNoQuestion("Do you want to only list hosts that are running (and not in your slice)? y/n");
			trigger(new GetHostsNotInSliceRequest(cred, forceDownload), plab
					.getPositive(PLabPort.class));
		}

		private void listHostsRegisteredForThisSlice() {
			System.out.println();
			boolean forceDownload = yesNoQuestion("Do you want to force the download of the list of hosts from PlanetLab Central? (y/n):");
			trigger(new GetHostsInSliceRequest(cred, forceDownload), plab
					.getPositive(PLabPort.class));
		}

		private void rankHostsUsingCoMon() {
			boolean forceDownload = yesNoQuestion("Do you want to force a fresh download of hosts from PlanetLab Central? y/n");

			System.out.println("Here is the list of stats to choose from: ");
			for (String s : CoMonStats.STATS) {
				System.out.print(s + ", ");
			}

			System.out.println("Here is the list of stats to choose from: ");
			List<String> stats = new ArrayList<String>();

			System.out
					.println("Enter 'x' to finish entering stats. Enter a stat now:");
			String entered = scanner.next();

			List<String> validStats = Arrays.asList(CoMonStats.STATS);

			while (entered.compareToIgnoreCase("x") != 0) {
				if (validStats.contains(entered) == false) {
					System.out.println("Error! Not a valid stat: " + entered);
				} else {
					System.out.println("Added the stat : " + entered);
					stats.add(entered);
				}
				entered = scanner.next();
			}

			trigger(new RankHostsUsingCoMonRequest(cred, forceDownload, stats
					.toArray(new String[(stats.size())])), plab
					.getPositive(PLabPort.class));

		}

		private void copyDaemonToHosts() {
			validateListHosts();

			System.out.println("There are " + connectedHosts.size()
					+ " connected hosts:");
			System.out.println();
			for (PLabHost h : connectedHosts) {
				System.out.print(h.getHostname() + " ");
			}
			System.out.println("Enter name from above connected hosts:");

			Set<String> hosts = enterValidHosts();

			String daemonJarFilename = getDaemonJarFile();
			File file = new File(daemonJarFilename);
			int pos = daemonJarFilename.lastIndexOf('/');

			for (String host : hosts) {
				PLabHost plHost = null;
				for (PLabHost h : connectedHosts) {
					if (h.getHostname().compareToIgnoreCase(host) == 0) {
						plHost = h;
					}
				}
				if (plHost == null) {
					System.out.println("Host not connected. Connect first : "
							+ host);
					continue;
				} else {
					int sessionId = plHost.getSessionId();

					// kompics/
					UploadFileRequest uploadJar = new UploadFileRequest(UUID.randomUUID(),
							sessionId, file, "~/", true, 10 * 1000.0, true);

					trigger(uploadJar, ssh.getPositive(SshPort.class));
				}
			}
		}

		private void connectToHosts() {
			if (validateListHosts() == false) {
				proceed();
				return;
			}
			Set<String> hosts = enterValidHosts();

			for (String h : hosts) {
				connectToHost(h, 0);
			}

			// Hack to wait for responses...
			try {
				Thread.currentThread().sleep(5 * 1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			proceed();
		}

		private Set<String> enterValidHosts() {
			System.out.println();
			System.out.println("Enter the hostnames.");
			System.out.println("Enter 'x' to finish entering hosts");
			String entered = scanner.next();
			Set<String> hosts = new HashSet<String>();
			while (entered.compareToIgnoreCase("x") != 0) {
				boolean found = false;
				for (Host h : availableHosts) {
					if (h.getHostname().compareToIgnoreCase(entered) == 0) {
						found = true;
					}
				}
				if (found == true) {
					hosts.add(entered);
				} else {
					System.out
							.println("This host is not available in your slice. Invalid hostname.");
				}
				entered = scanner.next();
			}
			return hosts;
		}

		private void addHostsToSlice() {
			Set<String> hosts = enterValidHosts();

			AddHostsToSliceRequest req = new AddHostsToSliceRequest(hosts);
			trigger(req, plab.getPositive(PLabPort.class));
		}

		private boolean selectHostsFile() {
			boolean succeed = true;
			System.out.println();
			System.out
					.println("Enter the full pathname of the file containing a list"
							+ "of comma-separated hosts in the format host[:port[:id]] ");
			String filename = scanner.next();
			try {
				Set<Host> hosts = HostsParser.parseHostsFile(filename);
			} catch (FileNotFoundException e) {
				System.out.println("File not found: " + e.getMessage());
				return false;
			} catch (HostsParserException e) {
				System.out.println("Hosts file not formatted correctly: "
						+ e.getMessage());
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
			System.out
					.print("\tEnter the number of peers to start at each host: ");
			return scanner.nextInt();
		}

		private Set<Host> getHosts() {
			int first, last;
			System.out.println("Enter the start of the range of hosts to use:");
			first = scanner.nextInt();
			System.out.println("Enter the end of the range of hosts to use:");
			last = scanner.nextInt();
			return MasterConfiguration.getHosts(first, last);
		}

		private int selectSshAuthMethod() {
			System.out.println();
			System.out
					.println("Enter a number to select an option from below:");
			System.out.println("\t1) username/password.");
			System.out.println("\t2) public-key authentication.");
			System.out.print("Enter your choice: ");
			return scanner.nextInt();
		}

		private int selectPlanetlabOption() {
			System.out.println();
			System.out
					.println("Enter a number to select an option from below:");
			System.out.println("\t1) change planet-lab credentials.");
			System.out.println("\t2) list hosts registered for this slice.");
			System.out
					.println("\t3) list hosts not registered for this slice.");
			System.out.println("\t4) add hosts to a slice.");
			System.out.println("\t5) rank hosts using CoMon statistics.");
			System.out.println("\t6) Connect using SSH to hosts.");
			System.out.println("\t7) Update CoMon stats.");
			System.out.println("\t8) scp (copy) daemon jar file to a host.");
			System.out.println("\t9) .");
			System.out.println("\t10) .");
			System.out.println("\t0) back");
			System.out.print("Enter your choice: ");
			return scanner.nextInt();
		}

		private int selectMenuOption() {
			System.out.println();
			System.out
					.println("Enter a number to select an option from below:");
			System.out.println("\t1) enter master.");
			System.out.println("\t2) enter planetlab.");
			System.out.println("\t0) exit program");
			System.out.print("Enter your choice: ");
			return scanner.nextInt();
		}

		private int selectMasterOption() {
			System.out.println();
			System.out
					.println("Enter a number to select an option from below:");
			System.out.println("\t0) back");
			System.out.println("\t1) list connected daemons.");
			System.out
					.println("\t2) specify a job, and list all daemons that have loaded it.");
			System.out
					.println("\t3) specify a daemon, and list all its loaded jobs.");
			System.out.println("\t4) load a job to all hosts.");
			System.out.println("\t5) load a job to selected hosts.");
			System.out.println("\t6) stop a job on all hosts.");
			System.out.println("\t7) copy daemon jar file to hosts.");
			System.out.println("\t8) connect to all hosts.");
			System.out
					.println("\t9) start a job on all hosts that have loaded the job.");
			System.out.println("\t11) shutdown all hosts.");
			System.out.print("Enter your choice: ");
			return scanner.nextInt();
		}

		public void exit() {
			this.finished.set(true);
		}

		private boolean validateListHosts() {
			if (availableHosts.size() == 0) {
				logger.warn("Warning: there are no available hosts.");
				return false;
			}
			return true;
		}

	}

	Handler<Fault> handleFault = new Handler<Fault>() {
		public void handle(Fault fault) {
			fault.getFault().printStackTrace(System.err);
		}
	};

	Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {

			if (ui == null) {
				ui = new UserInput();
				ui.start();
			}

		}
	};

}
