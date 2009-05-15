package se.sics.kompics.kdld.master;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.kdld.daemon.DaemonAddress;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.bootstrap.PeerEntry;
import se.sics.kompics.p2p.bootstrap.server.BootstrapServerInit;
import se.sics.kompics.p2p.bootstrap.server.CacheAddPeerRequest;
import se.sics.kompics.p2p.bootstrap.server.CacheEntry;
import se.sics.kompics.p2p.bootstrap.server.CacheEvictPeer;
import se.sics.kompics.p2p.bootstrap.server.CacheGetPeersRequest;
import se.sics.kompics.p2p.bootstrap.server.CacheGetPeersResponse;
import se.sics.kompics.p2p.bootstrap.server.CacheResetRequest;
import se.sics.kompics.p2p.epfd.EventuallyPerfectFailureDetector;
import se.sics.kompics.p2p.epfd.PeerFailureSuspicion;
import se.sics.kompics.p2p.epfd.StartProbingPeer;
import se.sics.kompics.p2p.epfd.SuspicionStatus;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
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
	
	Positive<EventuallyPerfectFailureDetector> epfd = positive(EventuallyPerfectFailureDetector.class);

	private final HashMap<String, HashSet<UUID>> outstandingTimeouts;
	private final HashMap<String, HashMap<Address, CacheEntry>> cache; 
																		
	private final HashMap<String, Long> cacheEpoch;

	private HashMap<DaemonAddress, Integer> registeredJobs; // <daemon, jobId>

//	private HashMap<Address, DaemonAddress> daemonPeers = new HashMap<Address, DaemonAddress>();
	private List<DaemonAddress> daemonPeers = new ArrayList<DaemonAddress>();

	private HashMap<Address, UUID> fdRequests;

	private long evictAfter;
	private Address self;
	private String webAddress;
	private int webPort;

	public Master() {
		this.cache = new HashMap<String, HashMap<Address, CacheEntry>>();
		this.cacheEpoch = new HashMap<String, Long>();

		outstandingTimeouts = new HashMap<String, HashSet<UUID>>();

		fdRequests = new HashMap<Address, UUID>();

		subscribe(handleInit, control);
		subscribe(handleStart, control);

		subscribe(handleWebRequest, web);
		subscribe(handleCacheResetRequest, net);
		subscribe(handleCacheAddPeerRequest, net);
		subscribe(handleCacheGetPeersRequest, net);
		
		subscribe(handleConnectMasterRequest, net);
		subscribe(handleDisconnectMasterRequest, net);
		

		subscribe(handlePeerFailureSuspicion, epfd);

		subscribe(handleCacheEvictPeer, timer);
		
		subscribe(handlePrintConnectedDameons, masterCommands);
		subscribe(handlePrintLoadedJobs, masterCommands);
		subscribe(handlePrintDaemonsWithLoadedJob, masterCommands);
	}

	private Handler<BootstrapServerInit> handleInit = new Handler<BootstrapServerInit>() {
		public void handle(BootstrapServerInit event) {
			evictAfter = event.getConfiguration().getCacheEvictAfter();
			self = event.getConfiguration().getBootstrapServerAddress();

			webPort = event.getConfiguration().getClientWebPort();
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

		private int selectOption()
	    {
	    	System.out.println("Kompics Master. Enter a number to select an option from below:");
	    	System.out.println("\t1) list connected daemons.");
	    	System.out.println("\t2) list all daemons with specified loaded job.");
	    	System.out.println("\t3) list loaded jobs for a specified daemon.");
	    	System.out.println();
	    	System.out.println("\t0) exit program");
	    	
	    	return scanner.nextInt();
	    }
	};

	
	private Handler<ConnectMasterRequest> handleConnectMasterRequest = new Handler<ConnectMasterRequest>() {
		public void handle(ConnectMasterRequest event) {

			DaemonAddress da = new DaemonAddress(event.getDaemonId(), event.getSource());
			if (daemonPeers.contains(da) == false)
			{
				daemonPeers.add(da);
			}
			trigger(new ConnectMasterResponse(true, 1000, self, event.getSource()), net);
		}
	};
	
	private Handler<DisconnectMasterRequest> handleDisconnectMasterRequest = new Handler<DisconnectMasterRequest>() {
		public void handle(DisconnectMasterRequest event) {

			DaemonAddress da = new DaemonAddress(event.getDaemonId(), event.getSource());
			daemonPeers.remove(da);
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
	
	private Handler<PrintDaemonsWithLoadedJob> handlePrintDaemonsWithLoadedJob = 
		new Handler<PrintDaemonsWithLoadedJob>() {
		public void handle(PrintDaemonsWithLoadedJob event) {
		
			
		}
	};
	
	private Handler<CacheResetRequest> handleCacheResetRequest = new Handler<CacheResetRequest>() {
		public void handle(CacheResetRequest event) {
			resetCache(event.getOverlay());
		}
	};

	private Handler<CacheAddPeerRequest> handleCacheAddPeerRequest = new Handler<CacheAddPeerRequest>() {
		public void handle(CacheAddPeerRequest event) {
			addPeerToCache(event.getPeerAddress(), event.getPeerOverlays());
			dumpCacheToLog();
		}
	};

	private Handler<CacheEvictPeer> handleCacheEvictPeer = new Handler<CacheEvictPeer>() {
		public void handle(CacheEvictPeer event) {
			// only evict if it was not refreshed in the meantime
			// which means the timer is not anymore outstanding
			HashSet<UUID> overlayEvictionTimoutIds = outstandingTimeouts.get(event.getOverlay());
			if (overlayEvictionTimoutIds != null) {
				if (overlayEvictionTimoutIds.contains(event.getTimeoutId())) {
					removePeerFromCache(event.getPeerAddress(), event.getOverlay(), event
							.getEpoch());
					outstandingTimeouts.remove(event.getTimeoutId());
				}
			}
			dumpCacheToLog();
		}
	};

	private Handler<CacheGetPeersRequest> handleCacheGetPeersRequest = new Handler<CacheGetPeersRequest>() {
		public void handle(CacheGetPeersRequest event) {
			int peersMax = event.getPeersMax();
			HashSet<PeerEntry> peers = new HashSet<PeerEntry>();
			long now = System.currentTimeMillis();
			String overlay = event.getOverlay();

			HashMap<Address, CacheEntry> overlayCache = cache.get(overlay);

			if (overlayCache != null) {
				Collection<CacheEntry> entries = overlayCache.values();
				ArrayList<CacheEntry> sorted = new ArrayList<CacheEntry>(entries);

				// get the most recent up to peersMax entries
				Collections.sort(sorted);
				for (CacheEntry cacheEntry : sorted) {
					PeerEntry peerEntry = new PeerEntry(overlay, cacheEntry.getOverlayAddress(),
							cacheEntry.getPeerAddress(), now - cacheEntry.getAddedAt(), now
									- cacheEntry.getRefreshedAt());
					peers.add(peerEntry);
					peersMax--;

					if (peersMax == 0)
						break;
				}
			}

			CacheGetPeersResponse response = new CacheGetPeersResponse(peers, event.getRequestId(),
					self, event.getSource());
			trigger(response, net);

			logger.debug("Responded with {} peers to peer {}", peers.size(), event.getSource());
		}
	};

	private Handler<WebRequest> handleWebRequest = new Handler<WebRequest>() {
		public void handle(WebRequest event) {
			logger.debug("Handling WebRequest");

			WebResponse response = new WebResponse(dumpCacheToHtml(event.getTarget()), event, 1, 1);
			trigger(response, web);
		}
	};

	private final void resetCache(String overlay) {
		// cancel all eviction timers for this overlay
		HashSet<UUID> overlayEvictionTimoutIds = outstandingTimeouts.get(overlay);
		if (overlayEvictionTimoutIds != null) {
			for (UUID timoutId : overlayEvictionTimoutIds) {
				CancelTimeout ct = new CancelTimeout(timoutId);
				trigger(ct, timer);
			}
			overlayEvictionTimoutIds.clear();
		}

		// reset cache
		HashMap<Address, CacheEntry> overlayCache = cache.get(overlay);
		if (overlayCache != null) {
			overlayCache.clear();
			Long epoch = cacheEpoch.get(overlay);
			cacheEpoch.put(overlay, 1 + epoch);
		} else {
			cache.put(overlay, new HashMap<Address, CacheEntry>());
			cacheEpoch.put(overlay, 1L);
			outstandingTimeouts.put(overlay, new HashSet<UUID>());
		}
		logger.debug("Cleared cache for " + overlay);
		dumpCacheToLog();
	}

	private final void addPeerToCache(Address address, Set<PeerEntry> overlays) {
		if (address != null) {
			long now = System.currentTimeMillis();

			for (PeerEntry peerEntry : overlays) {
				String overlay = peerEntry.getOverlay();

				HashMap<Address, CacheEntry> overlayCache = cache.get(overlay);
				if (overlayCache == null) {
					overlayCache = new HashMap<Address, CacheEntry>();
					cache.put(overlay, overlayCache);
					cacheEpoch.put(overlay, 1L);
					outstandingTimeouts.put(overlay, new HashSet<UUID>());
				}
				CacheEntry entry = overlayCache.get(address);
				if (entry == null) {
					// add a new entry
					entry = new CacheEntry(address, overlay, peerEntry.getOverlayAddress(), now,
							now);
					overlayCache.put(address, entry);

					// set a new eviction timeout
					ScheduleTimeout st = new ScheduleTimeout(evictAfter);
					st.setTimeoutEvent(new CacheEvictPeer(st, address, overlay, cacheEpoch
							.get(overlay)));

					UUID evictionTimerId = st.getTimeoutEvent().getTimeoutId();
					entry.setEvictionTimerId(evictionTimerId);
					outstandingTimeouts.get(overlay).add(evictionTimerId);
					trigger(st, timer);

					logger.debug("Added peer {}", address);
				} else {
					// update an existing entry
					entry.setRefreshedAt(now);

					// cancel an old eviction timeout, if it exists
					UUID oldTimeoutId = entry.getEvictionTimerId();
					if (oldTimeoutId != null) {
						trigger(new CancelTimeout(oldTimeoutId), timer);
						outstandingTimeouts.get(overlay).remove(oldTimeoutId);
					}
					// set a new eviction timeout
					ScheduleTimeout st = new ScheduleTimeout(evictAfter);
					st.setTimeoutEvent(new CacheEvictPeer(st, address, overlay, cacheEpoch
							.get(overlay)));

					UUID evictionTimerId = st.getTimeoutEvent().getTimeoutId();
					entry.setEvictionTimerId(evictionTimerId);
					outstandingTimeouts.get(overlay).add(evictionTimerId);
					trigger(st, timer);

					logger.debug("Refreshed peer {}", address);
				}
			}
		}
	}

	private final void removePeerFromCache(Address address, String overlay, long epoch) {
		long thisEpoch = cacheEpoch.get(overlay);
		if (address != null && epoch == thisEpoch) {
			cache.get(overlay).remove(address);

			logger.debug("Removed peer {}", address);
		}
	}

	private void dumpCacheToLog() {
		for (String overlay : cache.keySet()) {
			dumpCacheToLog(overlay);
		}
	}

	private void dumpCacheToLog(String overlay) {
		logger.info("Overlay {} now contains:", overlay);
		logger.info("Age=====Freshness==Peer address=========================");
		long now = System.currentTimeMillis();

		Collection<CacheEntry> entries = cache.get(overlay).values();
		ArrayList<CacheEntry> sorted = new ArrayList<CacheEntry>(entries);

		// get all peers in most recently added order
		Collections.sort(sorted);
		for (CacheEntry cacheEntry : sorted) {
			logger.info("{}\t{}\t  {}", new Object[] {
					durationToString(now - cacheEntry.getAddedAt()),
					durationToString(now - cacheEntry.getRefreshedAt()),
					cacheEntry.getPeerAddress() });
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
			for (String o : cache.keySet()) {
				sb.append("<a href=\"" + webAddress + o + "\">" + o + "</a>").append("<br>");
			}
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

		Collection<CacheEntry> entries = cache.get(overlay).values();
		ArrayList<CacheEntry> sorted = new ArrayList<CacheEntry>(entries);

		// get all peers in most recently added order
		Collections.sort(sorted);

		int count = 1;

		for (CacheEntry cacheEntry : sorted) {
			sb.append("<tr>");
			sb.append("<td><div align=\"center\">").append(count++);
			sb.append("</div></td>");
			sb.append("<td><div align=\"right\">");
			sb.append(durationToString(now - cacheEntry.getAddedAt()));
			sb.append("</div></td><td><div align=\"right\">");
			sb.append(durationToString(now - cacheEntry.getRefreshedAt()));
			sb.append("</div></td><td><div align=\"center\">");
			String webAddress = "http://" + cacheEntry.getPeerAddress().getIp().getHostAddress()
					+ ":" + webPort + "/" + cacheEntry.getPeerAddress().getId() + "/";
			sb.append("<a href=\"").append(webAddress).append("\">");
			sb.append(cacheEntry.getOverlayAddress().toString()).append("</a>");
			sb.append("</div></td><td><div align=\"left\">");
			sb.append(cacheEntry.getPeerAddress());
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

	private Handler<PeerFailureSuspicion> handlePeerFailureSuspicion = new Handler<PeerFailureSuspicion>() {
		public void handle(PeerFailureSuspicion event) {
			logger.debug("FAILURE_SUSPICION");

			if (event.getSuspicionStatus().equals(SuspicionStatus.SUSPECTED)) {
				// peer is suspected
//				DaemonAddress suspectedPeer = daemonPeers.get(event.getPeerAddress());
				DaemonAddress suspectedPeer = (DaemonAddress) event.getOverlayAddress();

				if (suspectedPeer == null
						|| !fdRequests.containsKey(suspectedPeer.getPeerAddress())) {
					// due to component concurrency it is possible that the FD
					// component sent us a suspicion event right after we sent
					// it a stop monitor request
					return;
				}

			}
		}
	};

	private final void registerDaemonForFailureDetection(CacheGetPeersRequest event) {

		Address addr = event.getSource();
//		DaemonAddress peer = daemonPeers.get(addr);

//		StartProbingPeer spp = new StartProbingPeer(addr, peer);
//		daemonPeers.put(addr, peer);
//		fdRequests.put(addr, spp.getRequestId());
//		trigger(spp, epfd);
	}

}
