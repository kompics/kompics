package se.sics.kompics.wan.master;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;
import se.sics.kompics.p2p.monitor.P2pMonitorConfiguration;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.wan.daemon.DaemonAddress;
import se.sics.kompics.web.Web;
import se.sics.kompics.web.WebRequest;
import se.sics.kompics.web.WebResponse;

/**
 * Takes in a SimulationScenario and a Hosts object. Sends messages to daemon
 * processes, which it assumes are already running. Monitors status of daemon
 * processes - like Bootstrap Server. Daemons register with the Master. Master
 * runs epfd to detect their failure.
 * 
 */
public class Master extends ComponentDefinition {

	private static final Logger logger = LoggerFactory.getLogger(Master.class);

	Positive<Network> net = positive(Network.class);
	Positive<Timer> timer = positive(Timer.class);
	Negative<Web> web = negative(Web.class);

	Negative<MasterCommands> masterCommands = negative(MasterCommands.class);

	// Positive<EventuallyPerfectFailureDetector> epfd =
	// positive(EventuallyPerfectFailureDetector.class);

	private final HashSet<UUID> outstandingTimeouts;
	private final HashMap<DaemonAddress, DaemonEntry> cache;

	private final Long cacheEpoch;
	// private final HashMap<String, Long> cacheEpoch;

	private HashMap<DaemonAddress, Integer> registeredJobs; // <daemon, jobId>

	// private HashMap<Address, UUID> fdRequests;

	private long evictAfter;
	private Address self;
	private String webAddress;
	private int webPort;

	private BootstrapConfiguration bootConfig;

	private P2pMonitorConfiguration monitorConfig;

	public Master() {
		this.cache = new HashMap<DaemonAddress, DaemonEntry>();
		this.cacheEpoch = 1L;

		outstandingTimeouts = new HashSet<UUID>();

		subscribe(handleInit, control);
		subscribe(handleStart, control);

		subscribe(handleWebRequest, web);
		subscribe(handleDisconnectMasterRequestMsg, net);
		subscribe(handleConnectMasterRequestMsg, net);
		subscribe(handleKeepAliveDaemonMsg, net);

		subscribe(handleCacheEvictDaemon, timer);

		subscribe(handlePrintConnectedDameons, masterCommands);
		subscribe(handlePrintLoadedJobs, masterCommands);
		subscribe(handlePrintDaemonsWithLoadedJob, masterCommands);
	}

	private Handler<MasterInit> handleInit = new Handler<MasterInit>() {
		public void handle(MasterInit event) {

			bootConfig = event.getBootConfig();
			monitorConfig = event.getMonitorConfig();

			evictAfter = event.getBootConfig().getCacheEvictAfter();
			self = event.getBootConfig().getBootstrapServerAddress();

			webPort = event.getBootConfig().getClientWebPort();
			webAddress = "http://" + self.getIp().getHostAddress() + ":" + webPort + "/"
					+ self.getId() + "/";

			logger.debug("Started");
			dumpCacheToLog();
		}
	};

	private Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {

			UserInput userInput = new UserInput(positive(MasterCommands.class));
			userInput.start();
		}
	};

	class UserInput extends Thread {
		private final Scanner scanner;
		private final Positive<MasterCommands> master;

		UserInput(Positive<MasterCommands> master) {
			scanner = new Scanner(System.in);
			this.master = master;
		}

		public void run() {
			boolean finishedInput = false;
			while (finishedInput == false) {
				switch (selectOption()) {
				case 1:
					trigger(new PrintConnectedDameons(), master);
					break;
				case 2:
					System.out.println("\tEnter job id: ");
					int jobId = scanner.nextInt();
					trigger(new PrintDaemonsWithLoadedJob(jobId), master);
					break;
				case 3:
					System.out.println("\tEnter daemon id: ");
					int daemonId = scanner.nextInt();
					trigger(new PrintLoadedJobs(daemonId), master);
					break;
				case 99:
					finishedInput = true;
					break;
				default:
					break;
				}
			}

		}

		private int selectOption() {
			System.out.println("Kompics Master. Enter a number to select an option from below:");
			System.out.println("\t1) list connected daemons.");
			System.out.println("\t2) list all daemons with specified loaded job.");
			System.out.println("\t3) list loaded jobs for a specified daemon.");
			System.out.println();
			System.out.println("\t0) exit program");

			return scanner.nextInt();
		}
	};

	private Handler<PrintConnectedDameons> handlePrintConnectedDameons = new Handler<PrintConnectedDameons>() {
		public void handle(PrintConnectedDameons event) {

		}
	};

	private Handler<PrintLoadedJobs> handlePrintLoadedJobs = new Handler<PrintLoadedJobs>() {
		public void handle(PrintLoadedJobs event) {

		}
	};

	private Handler<PrintDaemonsWithLoadedJob> handlePrintDaemonsWithLoadedJob = new Handler<PrintDaemonsWithLoadedJob>() {
		public void handle(PrintDaemonsWithLoadedJob event) {

		}
	};

	private Handler<DisconnectMasterRequestMsg> handleDisconnectMasterRequestMsg = new Handler<DisconnectMasterRequestMsg>() {
		public void handle(DisconnectMasterRequestMsg event) {

		}
	};
	
	private Handler<KeepAliveDaemonMsg> handleKeepAliveDaemonMsg = new Handler<KeepAliveDaemonMsg>() {
		public void handle(KeepAliveDaemonMsg event) {
			addDaemonToCache(event.getPeerAddress());
			dumpCacheToLog();
		}
	};

	private Handler<ConnectMasterRequestMsg> handleConnectMasterRequestMsg = new Handler<ConnectMasterRequestMsg>() {
		public void handle(ConnectMasterRequestMsg event) {

			DaemonAddress daemonAddress = new DaemonAddress(event.getDaemonId(), event.getSource());

			addDaemonToCache(daemonAddress);

			ConnectMasterResponseMsg response = new ConnectMasterResponseMsg(true, event
					.getRequestId(), self, event.getSource());
			trigger(response, net);

			logger.debug("Responded with connectMasterResponseMsg from {}", event.getSource());

			dumpCacheToLog();
		}
	};

	private Handler<CacheEvictDaemon> handleCacheEvictDaemon = new Handler<CacheEvictDaemon>() {
		public void handle(CacheEvictDaemon event) {
			// only evict if it was not refreshed in the meantime
			// which means the timer is not anymore outstanding

			if (outstandingTimeouts.contains(event.getTimeoutId())) {
				removePeerFromCache(event.getDaemonAddress(), event.getEpoch());
				outstandingTimeouts.remove(event.getTimeoutId());
			}
			dumpCacheToLog();
		}
	};

	private Handler<WebRequest> handleWebRequest = new Handler<WebRequest>() {
		public void handle(WebRequest event) {
			logger.debug("Handling WebRequest");

			WebResponse response = new WebResponse(dumpCacheToHtml(event.getTarget()), event, 1, 1);
			trigger(response, web);
		}
	};


	private final void addDaemonToCache(DaemonAddress address) {
		if (address != null) {
			long now = System.currentTimeMillis();

			DaemonEntry entry = cache.get(address);
			if (entry == null) {
				// add a new entry
				entry = new DaemonEntry(address, now, now);
				cache.put(address, entry);

				// set a new eviction timeout
				ScheduleTimeout st = new ScheduleTimeout(evictAfter);
				st.setTimeoutEvent(new CacheEvictDaemon(st, address, cacheEpoch));

				UUID evictionTimerId = st.getTimeoutEvent().getTimeoutId();
				entry.setEvictionTimerId(evictionTimerId);
				outstandingTimeouts.add(evictionTimerId);
				trigger(st, timer);

				logger.debug("Added peer {}", address);
			} else {
				// update an existing entry
				entry.setRefreshedAt(now);

				// cancel an old eviction timeout, if it exists
				UUID oldTimeoutId = entry.getEvictionTimerId();
				if (oldTimeoutId != null) {
					trigger(new CancelTimeout(oldTimeoutId), timer);
					outstandingTimeouts.remove(oldTimeoutId);
				}
				// set a new eviction timeout
				ScheduleTimeout st = new ScheduleTimeout(evictAfter);
				st.setTimeoutEvent(new CacheEvictDaemon(st, address, cacheEpoch));

				UUID evictionTimerId = st.getTimeoutEvent().getTimeoutId();
				entry.setEvictionTimerId(evictionTimerId);
				outstandingTimeouts.add(evictionTimerId);
				trigger(st, timer);

				logger.debug("Refreshed peer {}", address);
			}
		}
	}

	private final void removePeerFromCache(DaemonAddress address, long epoch) {
		long thisEpoch = cacheEpoch;
		if (address != null && epoch == thisEpoch) {
			cache.remove(address);
			logger.debug("Removed peer {}", address);
		}
	}

	private void dumpCacheToLog() {
		logger.info("Registered Daemons are:");
		logger.info("Age=====Freshness====Daemonaddress=========================");
		long now = System.currentTimeMillis();

		Collection<DaemonEntry> entries = cache.values();
		ArrayList<DaemonEntry> sorted = new ArrayList<DaemonEntry>(entries);

		// get all peers in most recently added order
		Collections.sort(sorted);
		for (DaemonEntry DaemonEntry : sorted) {
			logger.info("{}\t{}\t  {}", new Object[] {
					durationToString(now - DaemonEntry.getAddedAt()),
					durationToString(now - DaemonEntry.getRefreshedAt()),
					DaemonEntry.getDaemonAddress() });
		}

		logger.info("========================================================");
	}

	private String dumpCacheToHtml(String overlay) {
		if (!cache.containsKey(overlay)) {
			StringBuilder sb = new StringBuilder("<!DOCTYPE html PUBLIC \"-//W3C");
			sb.append("//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR");
			sb.append("/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http:");
			sb.append("//www.w3.org/1999/xhtml\"><head><meta http-equiv=\"Conten");
			sb.append("t-Type\" content=\"text/html; charset=utf-8\" />");
			sb.append("<title>Kompics P2P Bootstrap Server</title>");
			sb.append("<style type=\"text/css\"><!--.style2 {font-family: ");
			sb.append("Arial, Helvetica, sans-serif; color: #0099FF;}--></style>");
			sb.append("</head><body><h2 align=\"center\" class=\"style2\">");
			sb.append("Kompics P2P Bootstrap Overlays:</h2><br>");
			sb.append("<a href=\"" + webAddress + "\">daemons</a>").append("<br>");
			sb.append("</body></html>");
			return sb.toString();
		}

		StringBuilder sb = new StringBuilder("<!DOCTYPE html PUBLIC \"-//W3C");
		sb.append("//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR");
		sb.append("/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http:");
		sb.append("//www.w3.org/1999/xhtml\"><head><meta http-equiv=\"Conten");
		sb.append("t-Type\" content=\"text/html; charset=utf-8\" />");
		sb.append("<title>Kompics P2P Bootstrap Server</title>");
		sb.append("<style type=\"text/css\"><!--.style2 {font-family: ");
		sb.append("Arial, Helvetica, sans-serif; color: #0099FF;}--></style>");
		sb.append("</head><body><h2 align=\"center\" class=\"style2\">");
		sb.append("Kompics P2P Bootstrap Cache for " + overlay + "</h2>");
		sb.append("<table width=\"600\" border=\"0\" align=\"center\"><tr>");
		sb.append("<th class=\"style2\" width=\"100\" scope=\"col\">Count</th>");
		sb.append("<th class=\"style2\" width=\"80\" scope=\"col\">Age</th>");
		sb.append("<th class=\"style2\" width=\"120\" scope=\"col\">Freshness</th>");
		sb.append("<th class=\"style2\" width=\"300\" scope=\"col\">" + overlay + " id</th>");
		sb.append("<th class=\"style2\" width=\"300\" scope=\"col\">Peer address</th></tr>");
		long now = System.currentTimeMillis();

		Collection<DaemonEntry> entries = cache.values();
		ArrayList<DaemonEntry> sorted = new ArrayList<DaemonEntry>(entries);

		// get all peers in most recently added order
		Collections.sort(sorted);

		int count = 1;

		for (DaemonEntry DaemonEntry : sorted) {
			sb.append("<tr>");
			sb.append("<td><div align=\"center\">").append(count++);
			sb.append("</div></td>");
			sb.append("<td><div align=\"right\">");
			sb.append(durationToString(now - DaemonEntry.getAddedAt()));
			sb.append("</div></td><td><div align=\"right\">");
			sb.append(durationToString(now - DaemonEntry.getRefreshedAt()));
			sb.append("</div></td><td><div align=\"center\">");
			String webAddress = "http://"
					+ DaemonEntry.getDaemonAddress().getPeerAddress().getIp().getHostAddress()
					+ ":" + webPort + "/" + DaemonEntry.getDaemonAddress().getPeerAddress().getId()
					+ "/";
			sb.append("<a href=\"").append(webAddress).append("\">");
			sb.append(DaemonEntry.toString()).append("</a>");
			sb.append("</div></td><td><div align=\"left\">");
			sb.append(DaemonEntry.getDaemonAddress());
			sb.append("</div></td>");
			sb.append("</tr>");
		}

		sb.append("</table></body></html>");
		return sb.toString();
	}

	private String durationToString(long duration) {
		StringBuilder sb = new StringBuilder();

		// get duration in seconds
		duration /= 1000;

		int s = 0, m = 0, h = 0, d = 0, y = 0;
		s = (int) (duration % 60);
		// get duration in minutes
		duration /= 60;
		if (duration > 0) {
			m = (int) (duration % 60);
			// get duration in hours
			duration /= 60;
			if (duration > 0) {
				h = (int) (duration % 24);
				// get duration in days
				duration /= 24;
				if (duration > 0) {
					d = (int) (duration % 365);
					// get duration in years
					y = (int) (duration / 365);
				}
			}
		}

		boolean printed = false;

		if (y > 0) {
			sb.append(y).append("y");
			printed = true;
		}
		if (d > 0) {
			sb.append(d).append("d");
			printed = true;
		}
		if (h > 0) {
			sb.append(h).append("h");
			printed = true;
		}
		if (m > 0) {
			sb.append(m).append("m");
			printed = true;
		}
		if (s > 0 || printed == false) {
			sb.append(s).append("s");
		}

		return sb.toString();
	}

}
