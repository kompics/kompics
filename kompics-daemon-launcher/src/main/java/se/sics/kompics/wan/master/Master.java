package se.sics.kompics.wan.master;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;
import se.sics.kompics.p2p.monitor.P2pMonitorConfiguration;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.wan.daemon.DaemonAddress;
import se.sics.kompics.wan.daemon.JobLoadRequestMsg;
import se.sics.kompics.wan.daemon.JobLoadResponseMsg;
import se.sics.kompics.wan.daemon.JobStartRequestMsg;
import se.sics.kompics.wan.daemon.JobStopRequestMsg;
import se.sics.kompics.wan.daemon.JobsFoundMsg;
import se.sics.kompics.wan.job.Job;
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

	private final Logger logger = LoggerFactory.getLogger(Master.class);
	
	Positive<Network> net = positive(Network.class);
	Positive<Timer> timer = positive(Timer.class);
	Negative<Web> web = negative(Web.class);
	
	static SimulationScenario scenario = new SimulationScenario() {

		private static final long serialVersionUID = -1117143406424329L;
	};

	
	private Component userInput;

	private HashSet<UUID> outstandingTimeouts;
		
	/**
	 * (DaemonAddress, Cache-entry-for-daemon)
	 */
	private List<DaemonEntry> registeredDaemons;

	/**
	 * Updated when the cache is reset.
	 */
	private Long cacheEpoch;

	/**
	 * (ip-addr(hostname), <jobIds>)
	 */
	private HashMap<Integer, TreeSet<Integer>> loadedHostJobs; 
	
	/**
	 * (jobId, Job)
	 */
	private HashMap<Integer, Job> jobs;
	
	private long evictAfter;
	private Address self;
	private String webAddress;
	private int webPort;

	private BootstrapConfiguration bootConfig;
	private P2pMonitorConfiguration monitorConfig;

	public Master() {
		this.registeredDaemons = new ArrayList<DaemonEntry>();
		this.loadedHostJobs = new HashMap<Integer, TreeSet<Integer>>();
		this.jobs = new HashMap<Integer,Job>();
		this.cacheEpoch = 1L;
		this.userInput = create(UserInput.class);
		
		outstandingTimeouts = new HashSet<UUID>();

		subscribe(handleInit, control);
		subscribe(handleStart, control);

		subscribe(handleWebRequest, web);
		subscribe(handleDisconnectMasterRequestMsg, net);
		subscribe(handleConnectMasterRequestMsg, net);
		subscribe(handleKeepAliveDaemonMsg, net);
		subscribe(handleJobLoadResponseMsg, net);
		subscribe(handleJobsFoundMsg, net);

		subscribe(handleCacheEvictDaemon, timer);

		subscribe(handlePrintConnectedDameons, userInput.getPositive(MasterCommands.class));
		subscribe(handlePrintLoadedJobs, userInput.getPositive(MasterCommands.class));
		subscribe(handlePrintDaemonsWithLoadedJob, userInput.getPositive(MasterCommands.class));
		subscribe(handleInstallJobOnHosts, userInput.getPositive(MasterCommands.class));
		subscribe(handleStartJobOnHosts, userInput.getPositive(MasterCommands.class));
		subscribe(handleStopJobOnHosts, userInput.getPositive(MasterCommands.class));
		subscribe(handleShutdownDaemonRequest, userInput.getPositive(MasterCommands.class));
		
		connect(timer, userInput.getNegative(Timer.class));
	}

	private Handler<MasterInit> handleInit = new Handler<MasterInit>() {
		public void handle(MasterInit event) {

			self = event.getMaster();
			
			bootConfig = event.getBootConfig();
			monitorConfig = event.getMonitorConfig();

			evictAfter = event.getBootConfig().getCacheEvictAfter();

			webPort = event.getBootConfig().getClientWebPort();
			webAddress = "http://" + self.getIp().getHostAddress() + ":" + webPort + "/"
					+ self.getId() + "/";

			logger.debug("Started");
			dumpCacheToLog();
		}
	};

	private Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {
		}
	};


	private Handler<PrintConnectedDameons> handlePrintConnectedDameons = new Handler<PrintConnectedDameons>() {
		public void handle(PrintConnectedDameons event) {
				dumpCacheToLog();
		}
	};

	private Handler<PrintLoadedJobs> handlePrintLoadedJobs = new Handler<PrintLoadedJobs>() {
		public void handle(PrintLoadedJobs event) {

			int daemonId = event.getDaemonId();
			
			if (loadedHostJobs.size() == 0)
			{
				logger.info("No loaded jobs for daemon {}", daemonId);
			}
			else
			{
				logger.info("======== Start loaded jobs ========");
				
				TreeSet<Integer> jobIds = loadedHostJobs.get(daemonId);
				for (Integer i : jobIds)
				{
					logger.info("Job {} at {} (" + getDaemonIpFromId(daemonId)+")", 
							i.toString(), daemonId 
							);
				}
				logger.info("======== End loaded jobs ========");
			}
		}
	};

	
	private List<DaemonAddress> getDaemonsWithLoadedJob(int jobId)
	{
		List<DaemonAddress> daemonsFound = new ArrayList<DaemonAddress>();
		for (DaemonEntry d: registeredDaemons)
		{
			TreeSet<Integer> loadedJobsAtD = loadedHostJobs.get(d.getDaemonAddress().getDaemonId());
			if (loadedJobsAtD != null) {
				if (loadedJobsAtD.contains(jobId))
				{
					daemonsFound.add(d.getDaemonAddress());
				}
			}
		}
		return daemonsFound;
	}
	
	private Handler<PrintDaemonsWithLoadedJob> handlePrintDaemonsWithLoadedJob = new Handler<PrintDaemonsWithLoadedJob>() {
		public void handle(PrintDaemonsWithLoadedJob event) {
			int jobId = event.getJobId();
			
			List<DaemonAddress> daemonsFound = getDaemonsWithLoadedJob(jobId);

			if (daemonsFound.size() == 0)
			{
				logger.info("No daemons found that have loaded job " + jobId);
			}
			else {

				logger.info("======== {} daemons Found with loaded job {} ========", daemonsFound.size(), jobId);

				for (DaemonAddress d : daemonsFound)
				{
					logger.info(d.toString());
				}
				logger.info("======== End daemons Found with loaded job {} ========", jobId);
			}
		}
	};
	
	private Handler<ShutdownDaemonRequest> handleShutdownDaemonRequest = new Handler<ShutdownDaemonRequest>() {
		public void handle(ShutdownDaemonRequest event) {

			
			for (DaemonEntry dest : registeredDaemons)
			{
				trigger(new ShutdownDaemonRequestMsg(self, dest.getDaemonAddress()), net);
			}	
		}
	};

	
	private Handler<StartJobOnHosts> handleStartJobOnHosts = new Handler<StartJobOnHosts>() {
		public void handle(StartJobOnHosts event) {
			int jobId = event.getJobId();
			List<DaemonAddress> daemonsFound = getDaemonsWithLoadedJob(jobId);
			
			logger.info("Starting job. Num daemons found {} ", daemonsFound.size());
			for (DaemonAddress dest : daemonsFound)
			{
				logger.info("Starting job {} at {}", jobId, dest);
				trigger(new JobStartRequestMsg(jobId, event.getNumPeersPerHost(), 
						scenario, self, dest), net);
			}
		}
	};
	
	private Handler<StopJobOnHosts> handleStopJobOnHosts = new Handler<StopJobOnHosts>() {
		public void handle(StopJobOnHosts event) {
			int jobId = event.getJobId();
			List<DaemonAddress> daemonsFound = getDaemonsWithLoadedJob(jobId);
			
			logger.info("Stopping job. Num daemons found {} ", daemonsFound.size());
			for (DaemonAddress dest : daemonsFound)
			{
				logger.info("Stopping job {} at {}", jobId, dest);
				trigger(new JobStopRequestMsg(jobId, self, dest), net);
			}
		}
	};
	
	private Handler<InstallJobOnHosts> handleInstallJobOnHosts = new Handler<InstallJobOnHosts>() {
		public void handle(InstallJobOnHosts event) {
//			if (event.getHosts().size() == 0)
//			{
//				logger.warn("No hosts selected to install job on!");
//				return;
//			}
			
			jobs.put(event.getId(), event);
			
			for (DaemonEntry dest : registeredDaemons)
			{
				JobLoadRequestMsg job = new JobLoadRequestMsg(event, self, dest.getDaemonAddress());
				trigger(job, net);
				logger.info("Installing job {} on {}", job.getArtifactId(), dest);
			}
		}
	};
	
	private Handler<JobsFoundMsg> handleJobsFoundMsg = new Handler<JobsFoundMsg>() {
		public void handle(JobsFoundMsg event) {
			
			Set<Job> jobSet = event.getSetJobs();
			
			logger.info("Job(s) found on daemon {} :", event.getSetJobs().size(), event.getDaemonId());
			for (Job job : jobSet)
			{
				logger.info("{}", job.getId());
			}
			
			DaemonAddress src = new DaemonAddress(event.getDaemonId(), event.getSource());
			addJobs(src, jobSet);
		}
	};

	private void addJobs(DaemonAddress addr, Set<Job> jobSet)
	{
		for (Job job : jobSet)
		{
			addJob(addr, job);
		}
	}

	private TreeSet<Integer> getJobsForDaemon(DaemonAddress addr)
	{
		TreeSet<Integer> jobIds = loadedHostJobs.get(addr);
		if (jobIds == null)
		{
			jobIds = new TreeSet<Integer>();
		}
		return jobIds;
	}
	
	private DaemonAddress getDaemonAddressFromId(int id)
	{
		for (DaemonEntry d : registeredDaemons)
		{
			if (d.getDaemonAddress().getDaemonId() == id)
			{
				return d.getDaemonAddress();
			}
		}
		return null;
	}

	private String getDaemonHostnameFromId(int id)
	{
		DaemonAddress d = getDaemonAddressFromId(id);
		if (d != null)
		{
			return d.getPeerAddress().getIp().getHostName();
		}
		return ("Hostname lookup failed");
	}
	
	private String getDaemonIpFromId(int id)
	{
		DaemonAddress d = getDaemonAddressFromId(id);
		if (d != null)
		{
			return d.getPeerAddress().getIp().getHostAddress();
		}
		return ("Hostname lookup failed");
	}
	
	private void addJob(DaemonAddress addr, Job job)
	{
		jobs.put(job.getId(), job);
		TreeSet<Integer> jobIds = getJobsForDaemon(addr);
		jobIds.add(job.getId());
		loadedHostJobs.put(addr.getDaemonId(), jobIds);
	}
	
	private Handler<JobLoadResponseMsg> handleJobLoadResponseMsg = new Handler<JobLoadResponseMsg>() {
		public void handle(JobLoadResponseMsg event) {

			logger.info("JobLoadResponse received for {} was {}", 
					event.getJobId(), event.getStatus());
			
			// event.getSource().getIp().getCanonicalHostName();
			TreeSet<Integer> jobsAtDaemon = loadedHostJobs.get(event.getDaemonId());
			if (jobsAtDaemon == null)
			{
				jobsAtDaemon = new TreeSet<Integer>();
			}
			jobsAtDaemon.add(event.getJobId());
			loadedHostJobs.put(event.getDaemonId(), jobsAtDaemon);
		}
	};
	
	
	
	
	private Handler<DisconnectMasterRequestMsg> handleDisconnectMasterRequestMsg = new Handler<DisconnectMasterRequestMsg>() {
		public void handle(DisconnectMasterRequestMsg event) {
			logger.info("Daemon disconnecting: {}", event.getDaemon());
			removePeerFromCache(event.getDaemon(), cacheEpoch);
		}
	};
	
	private Handler<KeepAliveDaemonMsg> handleKeepAliveDaemonMsg = new Handler<KeepAliveDaemonMsg>() {
		public void handle(KeepAliveDaemonMsg event) {
			addDaemonToCache(event.getPeerAddress());
			logger.debug("Refreshed connection to daemon {}", event.getPeerAddress().getDaemonId());
		}
	};

	/**
	 * Daemons send ConnectMasterRequestMsg events to the Master. They retry if they
	 * don't receive a responseMsg before a timeout expires. Retry is safe, as it 
	 * just updates the daemon's timestamp in the cache.
	 * After registration, the daemon sends KeepAliveDaemonMsg events that don't
	 * require a response msg. 
	 */
	private Handler<ConnectMasterRequestMsg> handleConnectMasterRequestMsg = new Handler<ConnectMasterRequestMsg>() {
		public void handle(ConnectMasterRequestMsg event) {

			logger.debug("Received msg from {}", event.getSource());
			
			DaemonAddress daemonAddress = new DaemonAddress(event.getDaemonId(), event.getSource());

			addDaemonToCache(daemonAddress);

			ConnectMasterResponseMsg response = new ConnectMasterResponseMsg(true, event
					.getRequestId(), self, event.getSource());
			trigger(response, net);
			logger.info("Request-id: {}", event.getRequestId());
			
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

			WebResponse response = new WebResponse(dumpCacheToHtml(), event, 1, 1);
			trigger(response, web);
		}
	};


	private DaemonEntry findDaemonEntry(DaemonAddress address)
	{
		for (DaemonEntry de : registeredDaemons)
		{
			if (de.getDaemonAddress().compareTo(address) == 0)
			{
				return de;
			}
		}
		return null;
	}
	
	private final void addDaemonToCache(DaemonAddress address) {
		if (address != null) {
			long now = System.currentTimeMillis();

			DaemonEntry entry = findDaemonEntry(address);
			if (entry == null) {
				// add a new entry
				// XXX look-up groupId for this host in table, and add to constructor
				entry = new DaemonEntry(address, now, now);
				registeredDaemons.add(entry);

				// set a new eviction timeout
				ScheduleTimeout st = new ScheduleTimeout(evictAfter);
				st.setTimeoutEvent(new CacheEvictDaemon(st, address, cacheEpoch));

				UUID evictionTimerId = st.getTimeoutEvent().getTimeoutId();
				entry.setEvictionTimerId(evictionTimerId);
				outstandingTimeouts.add(evictionTimerId);
				trigger(st, timer);

				logger.debug("Updated cache with peer {}", address);
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
			registeredDaemons.remove(address);
			logger.info("Removed peer {}", address);
		}
	}

	private void dumpCacheToLog() {
		if (registeredDaemons.size() == 0)
		{
			return;
		}
		logger.info("Age=====Freshness====Daemonaddress====ID===");
		long now = System.currentTimeMillis();

		Collections.sort(registeredDaemons, new DaemonAgeComparator());
		for (DaemonEntry daemonEntry : registeredDaemons) {
			logger.info("{}\t{}\t  {} : id={}", new Object[] {
					durationToString(now - daemonEntry.getAddedAt()),
					durationToString(now - daemonEntry.getRefreshedAt()),
					daemonEntry.getHostname(), 
					daemonEntry.getDaemonAddress().getDaemonId() });
		}

		logger.info("=========================================");
	}

	private String dumpCacheToHtml() {

		StringBuilder sb = new StringBuilder("<!DOCTYPE html PUBLIC \"-//W3C");
		sb.append("//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR");
		sb.append("/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http:");
		sb.append("//www.w3.org/1999/xhtml\"><head><meta http-equiv=\"Conten");
		sb.append("t-Type\" content=\"text/html; charset=utf-8\" />");
		sb.append("<title>Kompics P2P Bootstrap Server</title>");
		sb.append("<style type=\"text/css\"><!--.style2 {font-family: ");
		sb.append("Arial, Helvetica, sans-serif; color: #0099FF;}--></style>");
		sb.append("</head><body><h2 align=\"center\" class=\"style2\">");
		sb.append("Kompics P2P Bootstrap Cache </h2>");
		sb.append("<table width=\"600\" border=\"0\" align=\"center\"><tr>");
		sb.append("<th class=\"style2\" width=\"100\" scope=\"col\">Count</th>");
		sb.append("<th class=\"style2\" width=\"80\" scope=\"col\">Age</th>");
		sb.append("<th class=\"style2\" width=\"120\" scope=\"col\">Freshness</th>");
		sb.append("<th class=\"style2\" width=\"300\" scope=\"col\">Peer address</th></tr>");
		long now = System.currentTimeMillis();

		Collections.sort(registeredDaemons, new DaemonAgeComparator());

		int count = 1;

		for (DaemonEntry DaemonEntry : registeredDaemons) {
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

	private final void resetCache() {
		// cancel all eviction timers for this overlay
		HashSet<UUID> overlayEvictionTimoutIds = outstandingTimeouts;
		if (overlayEvictionTimoutIds != null) {
			for (UUID timoutId : overlayEvictionTimoutIds) {
				CancelTimeout ct = new CancelTimeout(timoutId);
				trigger(ct, timer);
			}
			overlayEvictionTimoutIds.clear();
		}

		// reset cache
		if (registeredDaemons != null) {
			registeredDaemons.clear();
			cacheEpoch += 1L;
		} else {
			registeredDaemons = new ArrayList<DaemonEntry>();
			cacheEpoch = 1L;
			outstandingTimeouts= new HashSet<UUID>();
		}
		logger.debug("Cleared daemon cache.");
		dumpCacheToLog();
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
