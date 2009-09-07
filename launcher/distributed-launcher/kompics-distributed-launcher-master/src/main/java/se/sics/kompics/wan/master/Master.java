package se.sics.kompics.wan.master;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import se.sics.kompics.wan.masterdaemon.events.ShutdownDaemonRequestMsg;
import se.sics.kompics.wan.master.events.ShutdownDaemonRequest;
import se.sics.kompics.wan.master.events.CacheEvictDaemon;
import se.sics.kompics.wan.master.events.GetDaemonsWithLoadedJobRequest;
import se.sics.kompics.wan.master.events.InstallJobOnHostsRequest;
import se.sics.kompics.wan.master.events.GetConnectedDameonsRequest;
import se.sics.kompics.wan.master.events.StartJobOnHostsRequest;
import se.sics.kompics.wan.master.events.GetLoadedJobsForDaemonRequest;
import se.sics.kompics.wan.master.events.KeepAliveDaemonMsg;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.wan.daemonmaster.events.ConnectMasterRequestMsg;
import se.sics.kompics.wan.daemonmaster.events.ConnectMasterResponseMsg;
import se.sics.kompics.wan.daemonmaster.events.DisconnectMasterRequestMsg;
import se.sics.kompics.wan.daemonmaster.events.JobFoundMsg;
import se.sics.kompics.wan.job.Job;
import se.sics.kompics.wan.job.JobExecRequest;
import se.sics.kompics.wan.job.JobExecResponse;
import se.sics.kompics.wan.job.JobLoadResponse;
import se.sics.kompics.wan.job.JobStopRequest;
import se.sics.kompics.wan.master.events.ConnectedDaemonNotification;
import se.sics.kompics.wan.master.events.GetConnectedDaemonsResponse;
import se.sics.kompics.wan.master.events.GetLoadedJobsForDaemonResponse;
import se.sics.kompics.wan.master.events.GetLoadedJobsRequest;
import se.sics.kompics.wan.master.events.GetLoadedJobsResponse;
import se.sics.kompics.wan.master.events.InstallJobOnHostsResponse;
import se.sics.kompics.wan.master.events.JobsFound;
import se.sics.kompics.wan.masterdaemon.events.DaemonAddress;
import se.sics.kompics.wan.masterdaemon.events.JobExecRequestMsg;
import se.sics.kompics.wan.masterdaemon.events.JobExecResponseMsg;
import se.sics.kompics.wan.masterdaemon.events.JobLoadRequestMsg;
import se.sics.kompics.wan.masterdaemon.events.JobLoadResponseMsg;
import se.sics.kompics.wan.masterdaemon.events.JobStartRequestMsg;
import se.sics.kompics.wan.masterdaemon.events.JobStopRequestMsg;
import se.sics.kompics.wan.ssh.Host;
import se.sics.kompics.wan.util.PomUtils;
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
    static SimulationScenario scenario = new SimulationScenario() {

        private static final long serialVersionUID = -1117143406424329L;
    };
    Negative<MasterPort> masterPort = negative(MasterPort.class);
    Positive<Network> net = positive(Network.class);
    Positive<Timer> timer = positive(Timer.class);
    Negative<Web> web = negative(Web.class);
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
    private Map<Integer, InstallJobOnHostsRequest> installingJobs =
            new HashMap<Integer, InstallJobOnHostsRequest>();

    private Map<Integer, JobExecRequest> executingJobs = new HashMap<Integer, JobExecRequest>();

    /**
     * (jobId, Job)
     */
    private HashMap<Integer, Job> jobs;
    private long evictAfter;
    private Address self;
    private String webAddress;
    private int webPort;
    private BootstrapConfiguration bootConfig;
//    private P2pMonitorConfiguration monitorConfig;

    public Master() {
        this.registeredDaemons = new ArrayList<DaemonEntry>();
        this.loadedHostJobs = new HashMap<Integer, TreeSet<Integer>>();
        this.jobs = new HashMap<Integer, Job>();
        this.cacheEpoch = 1L;

        outstandingTimeouts = new HashSet<UUID>();

        subscribe(handleInit, control);
        subscribe(handleStart, control);

        subscribe(handleWebRequest, web);
        subscribe(handleDisconnectMasterRequestMsg, net);
        subscribe(handleConnectMasterRequestMsg, net);
        subscribe(handleKeepAliveDaemonMsg, net);
        subscribe(handleJobLoadResponseMsg, net);
        subscribe(handleJobsFoundMsg, net);
        subscribe(handleJobExecResponseMsg, net);

        subscribe(handleCacheEvictDaemon, timer);

        subscribe(handlePrintConnectedDameons, masterPort);
        subscribe(handlePrintLoadedJobs, masterPort);
        subscribe(handlePrintDaemonsWithLoadedJob, masterPort);
        subscribe(handleInstallJobOnHosts, masterPort);
        subscribe(handleStartJobOnHosts, masterPort);
        subscribe(handleJobStopRequest, masterPort);
        subscribe(handleShutdownDaemonRequest, masterPort);
        subscribe(handleGetLoadedJobsRequest, masterPort);
        subscribe(handleJobExecRequest, masterPort);
    }
    private Handler<MasterInit> handleInit = new Handler<MasterInit>() {

        public void handle(MasterInit event) {

            self = event.getMaster();

            bootConfig = event.getBootConfig();
//            monitorConfig = event.getMonitorConfig();

            evictAfter = event.getBootConfig().getCacheEvictAfter();

            webPort = event.getBootConfig().getClientWebPort();
            webAddress = "http://" + self.getIp().getHostAddress() + ":" + webPort + "/" + self.getId() + "/";

            logger.debug("Master listening on {}", self.toString());
            dumpCacheToLog();
        }
    };
    private Handler<Start> handleStart = new Handler<Start>() {

        public void handle(Start event) {
        }
    };
    private Handler<GetConnectedDameonsRequest> handlePrintConnectedDameons = new Handler<GetConnectedDameonsRequest>() {

        public void handle(GetConnectedDameonsRequest event) {
            dumpCacheToLog();
        }
    };
    private Handler<GetLoadedJobsForDaemonRequest> handlePrintLoadedJobs = new Handler<GetLoadedJobsForDaemonRequest>() {

        public void handle(GetLoadedJobsForDaemonRequest event) {

            int daemonId = event.getDaemonId();

            if (loadedHostJobs.size() == 0) {
                logger.info("No loaded jobs for daemon {}", daemonId);
            } else {
                logger.info("======== Start loaded jobs ========");

                TreeSet<Integer> jobIds = loadedHostJobs.get(daemonId);
                for (Integer i : jobIds) {
                    logger.info("Job {} at {} (" + getDaemonIpFromId(daemonId) + ")", i.toString(), daemonId);
                }
                logger.info("======== End loaded jobs ========");
            }

            trigger(new GetLoadedJobsForDaemonResponse(event, getDaemonAddressFromId(daemonId),
                    new HashSet<Job>(jobs.values())),
                    masterPort);
        }
    };

    private List<DaemonAddress> getDaemonsWithLoadedJob(int jobId) {
        List<DaemonAddress> daemonsFound = new ArrayList<DaemonAddress>();
        for (DaemonEntry d : registeredDaemons) {
            TreeSet<Integer> loadedJobsAtD = loadedHostJobs.get(d.getDaemonAddress().getDaemonId());
            if (loadedJobsAtD != null) {
                if (loadedJobsAtD.contains(jobId)) {
                    daemonsFound.add(d.getDaemonAddress());
                }
            }
        }
        return daemonsFound;
    }
    private Handler<GetDaemonsWithLoadedJobRequest> handlePrintDaemonsWithLoadedJob = new Handler<GetDaemonsWithLoadedJobRequest>() {

        public void handle(GetDaemonsWithLoadedJobRequest event) {
            int jobId = event.getJobId();

            List<DaemonAddress> daemonsFound = getDaemonsWithLoadedJob(jobId);

            if (daemonsFound.size() == 0) {
                logger.info("No daemons found that have loaded job " + jobId);
            } else {

                logger.info(
                        "======== {} daemons Found with loaded job {} ========",
                        daemonsFound.size(), jobId);

                for (DaemonAddress d : daemonsFound) {
                    logger.info(d.toString());
                }
                logger.info(
                        "======== End daemons Found with loaded job {} ========",
                        jobId);
            }


        }
    };
    private Handler<ShutdownDaemonRequest> handleShutdownDaemonRequest = new Handler<ShutdownDaemonRequest>() {

        public void handle(ShutdownDaemonRequest event) {

            for (DaemonEntry dest : registeredDaemons) {
                trigger(new ShutdownDaemonRequestMsg(self, dest.getDaemonAddress()), net);
            }
        }
    };

      private Handler<JobExecResponseMsg> handleJobExecResponseMsg = new Handler<JobExecResponseMsg>() {

        public void handle(JobExecResponseMsg event) {

            int jobId = event.getJobId();
            JobExecRequest request = executingJobs.get(jobId);
            if (request != null) {
                trigger(new JobExecResponse(request, jobId, event.getStatus()), masterPort);
            }
            else {
                logger.warn("Could not find request for jobId: ", jobId);
            }
        }
      };

        private Handler<JobExecRequest> handleJobExecRequest = new Handler<JobExecRequest>() {

        public void handle(JobExecRequest event) {
            int jobId = PomUtils.generateJobId(event.getGroupId(), event.getArtifactId(), event.getVersion());

            executingJobs.put(jobId, event);

            DaemonEntry addr = findDaemonFromHostname(event.getHostname());
            if (addr != null) {
                logger.info("Starting job {} at {}", jobId, addr.getDaemonAddress());
                trigger(new JobExecRequestMsg(jobId,
                        event.getHostname(), event.getNumPeers(),
                        event.getPort(), event.getWebPort(),
                        event.getBootHost(), event.getBootPort(), event.getBootWebPort(),
                        event.getMonitorHost(), event.getMonitorPort(), event.getMonitorWebPort(),
                        event.getMainClass(), event.getArgs(), 
                        self,
                        addr.getDaemonAddress()), net);
            }
            else {
                logger.warn("Daemon for {} was not registered. Cannot start job.", event.getHostname());
            }
        }
    };

    private Handler<StartJobOnHostsRequest> handleStartJobOnHosts = new Handler<StartJobOnHostsRequest>() {

        public void handle(StartJobOnHostsRequest event) {
            int jobId = event.getJobId();
            List<DaemonAddress> daemonsFound = getDaemonsWithLoadedJob(jobId);

            logger.info("Starting job. Num daemons found {} ", daemonsFound.size());
            for (DaemonAddress dest : daemonsFound) {
                logger.info("Starting job {} at {}", jobId, dest);
                trigger(new JobStartRequestMsg(jobId, event.getNumPeersPerHost(), scenario, self, dest), net);
            }
        }
    };
    private Handler<JobStopRequest> handleJobStopRequest = new Handler<JobStopRequest>() {

        public void handle(JobStopRequest event) {
            int jobId = event.getJobId();

            String hostname = event.getHostname();

            DaemonEntry daemon = findDaemonFromHostname(hostname);


//            List<DaemonAddress> daemonsFound = getDaemonsWithLoadedJob(jobId);

            logger.info("Stopping job. in Master");
            if (daemon == null) {
//                trigger(new StopJobOnHostsResponse(event))
                
                // TODO send response
                logger.warn("Could not find host for job to stop at daemons: {}, {}", jobId, hostname);
            }
            else {
//                for (DaemonAddress dest : daemonsFound) {
                    logger.info("Found daemon!! Stopping job {} at {}", jobId, daemon.getDaemonAddress());
                    trigger(new JobStopRequestMsg(jobId, self, daemon.getDaemonAddress()), net);
//                }
            }
        }
    };
    private Handler<InstallJobOnHostsRequest> handleInstallJobOnHosts = new Handler<InstallJobOnHostsRequest>() {

        public void handle(InstallJobOnHostsRequest event) {

            // already installing this job
            if (installingJobs.containsKey(event.getId()) == true) {
                logger.warn("Already installing this job");
//                trigger(new InstallJobOnHostsResponse(event, JobLoadResponse.Status.DUPLICATE,
//                        "Duplicate request to load a job. Job loading in progress"),
//                        masterPort);
            }

            if (event.getHosts().size() == 0) {
                logger.warn("No hosts selected to install job on!");
                return;
            }

            if (registeredDaemons.size() == 0) {
                for (Host host : event.getHosts()) {
                    if (host.getIp() == null) {
                        try {
                            host.setIp(InetAddress.getByName(host.getHostname()));
                        } catch (UnknownHostException ex) {
                            java.util.logging.Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    trigger(new InstallJobOnHostsResponse(event, host.getIp(),
                            event.getId(), JobLoadResponse.Status.FAIL,
                            "No daemons are connected. Cannot load job!"),
                            masterPort);
                }
                return;
            }

            jobs.put(event.getId(), event);

            for (Host h : event.getHosts()) {

                if (h.getIp() == null) {
                    try {
                        h.setIp(InetAddress.getByName(h.getHostname()));
                    } catch (UnknownHostException ex) {
                        java.util.logging.Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
                        continue;
                    }
                }


                for (DaemonEntry dest : registeredDaemons) {
                    // XXX this assumes they have the same hostname - may not always work
                    if (dest.getDaemonAddress().getPeerAddress().getIp().equals(h.getIp())) {
                        JobLoadRequestMsg job = new JobLoadRequestMsg(event,
                                event.isHideMavenOutput(), self, dest.getDaemonAddress());
                        trigger(job, net);
                        logger.info("Installing job {} on {}", job.getArtifactId(), dest);

                        installingJobs.put(job.getJobId(), event);
                    }
                }

            }

        }
    };
    private Handler<GetLoadedJobsRequest> handleGetLoadedJobsRequest = new Handler<GetLoadedJobsRequest>() {

        public void handle(GetLoadedJobsRequest event) {

            logger.debug("Received GetLoadedJobsRequest");
            trigger(new GetLoadedJobsResponse(event, new HashSet<Job>(jobs.values())), masterPort);
        }
    };
    private Handler<JobFoundMsg> handleJobsFoundMsg = new Handler<JobFoundMsg>() {

        public void handle(JobFoundMsg event) {

            Job job = event.getJob();

            logger.info("Job {} found on daemon {} :", event.getJob().getId(), event.getDaemonId());

            DaemonAddress src = new DaemonAddress(event.getDaemonId(), event.getSource());
            addJob(src, job);

            trigger(new JobsFound(src.getPeerAddress(), job), masterPort);
        }
    };

    private void addJobs(DaemonAddress addr, Set<Job> jobSet) {
        for (Job job : jobSet) {
            addJob(addr, job);
        }
    }

    private TreeSet<Integer> getJobsForDaemon(DaemonAddress addr) {
        TreeSet<Integer> jobIds = loadedHostJobs.get(addr);
        if (jobIds == null) {
            jobIds = new TreeSet<Integer>();
        }
        return jobIds;
    }

    private DaemonAddress getDaemonAddressFromId(int id) {
        for (DaemonEntry d : registeredDaemons) {
            if (d.getDaemonAddress().getDaemonId() == id) {
                return d.getDaemonAddress();
            }
        }
        return null;
    }

    private String getDaemonHostnameFromId(int id) {
        DaemonAddress d = getDaemonAddressFromId(id);
        if (d != null) {
            return d.getPeerAddress().getIp().getHostName();
        }
        return ("Hostname lookup failed");
    }

    private String getDaemonIpFromId(int id) {
        DaemonAddress d = getDaemonAddressFromId(id);
        if (d != null) {
            return d.getPeerAddress().getIp().getHostAddress();
        }
        return ("Hostname lookup failed");
    }

    private void addJob(DaemonAddress addr, Job job) {
        jobs.put(job.getId(), job);
        TreeSet<Integer> jobIds = getJobsForDaemon(addr);
        jobIds.add(job.getId());
        loadedHostJobs.put(addr.getDaemonId(), jobIds);
    }
    private Handler<JobLoadResponseMsg> handleJobLoadResponseMsg = new Handler<JobLoadResponseMsg>() {

        public void handle(JobLoadResponseMsg event) {

            logger.info("JobLoadResponse received for {} was {}", event.getJobId(), event.getStatus());

            // event.getSource().getIp().getCanonicalHostName();
            TreeSet<Integer> jobsAtDaemon = loadedHostJobs.get(event.getDaemonId());
            if (jobsAtDaemon == null) {
                jobsAtDaemon = new TreeSet<Integer>();
            }
            jobsAtDaemon.add(event.getJobId());
            loadedHostJobs.put(event.getDaemonId(), jobsAtDaemon);

            InstallJobOnHostsRequest req = installingJobs.get(event.getJobId());

//            String hostname = event.getSource().getIp().getCanonicalHostName();

            if (req != null) {
                trigger(new InstallJobOnHostsResponse(req, event.getSource().getIp(), event.getJobId(), event.getStatus(),
                        Integer.toString(event.getJobId())), masterPort);
            } else {
                logger.warn("Could not find InstallJob request for job {}", event.getJobId());
            }
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
     * Daemons send ConnectMasterRequestMsg events to the Master. They retry if
     * they don't receive a responseMsg before a timeout expires. Retry is safe,
     * as it just updates the daemon's timestamp in the cache. After
     * registration, the daemon sends KeepAliveDaemonMsg events that don't
     * require a response msg.
     */
    private Handler<ConnectMasterRequestMsg> handleConnectMasterRequestMsg = new Handler<ConnectMasterRequestMsg>() {

        public void handle(ConnectMasterRequestMsg event) {

            logger.debug("CONNECT MASTER: Received msg from {}", event.getSource());

            DaemonAddress daemonAddress = new DaemonAddress(
                    event.getDaemonId(), event.getSource());

            addDaemonToCache(daemonAddress);

            ConnectMasterResponseMsg response = new ConnectMasterResponseMsg(
                    true, event.getRequestId(), self, event.getSource());
            trigger(response, net);
            logger.info("Request-id: {}", event.getRequestId());

            logger.debug("Responded with connectMasterResponseMsg from {}",
                    event.getSource());

            String daemonHost = event.getSource().getIp().getHostName();
            // tell higher application layers on masterPort a daemon has connected
            trigger(new ConnectedDaemonNotification(daemonHost), masterPort);

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

            WebResponse response = new WebResponse(dumpCacheToHtml(), event, 1,
                    1);
            trigger(response, web);
        }
    };

    private DaemonEntry findDaemonFromHostname(String hostname) {
        DaemonEntry entry = null;
        try {
            InetAddress addr = InetAddress.getByName(hostname);

            for (DaemonEntry dest : registeredDaemons) {
                if (dest.getDaemonAddress().getPeerAddress().getIp().equals(addr)) {
                    return dest;
                }
            }
        } catch (UnknownHostException ex) {
            java.util.logging.Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
        }


        return entry;
    }

    private DaemonEntry findDaemonFromAddress(Address address) {
        for (DaemonEntry de : registeredDaemons) {
            if (de.getDaemonAddress().getPeerAddress().compareTo(address) == 0) {
                return de;
            }
        }
        return null;
    }

    private DaemonEntry findDaemonEntry(DaemonAddress address) {
        for (DaemonEntry de : registeredDaemons) {
            if (de.getDaemonAddress().compareTo(address) == 0) {
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
                // XXX look-up groupId for this host in table, and add to
                // constructor
                entry = new DaemonEntry(address, now, now);
                registeredDaemons.add(entry);

                // set a new eviction timeout
                ScheduleTimeout st = new ScheduleTimeout(evictAfter);
                st.setTimeoutEvent(new CacheEvictDaemon(st, address,
                        cacheEpoch));

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
                st.setTimeoutEvent(new CacheEvictDaemon(st, address,
                        cacheEpoch));

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
        if (registeredDaemons.size() == 0) {
            return;
        }
        logger.info("Age=====Freshness====Daemonaddress====ID===");
        long now = System.currentTimeMillis();

        Collections.sort(registeredDaemons, new DaemonAgeComparator());
        for (DaemonEntry daemonEntry : registeredDaemons) {
            logger.info("{}\t{}\t  {} : id={}", new Object[]{
                        durationToString(now - daemonEntry.getAddedAt()),
                        durationToString(now - daemonEntry.getRefreshedAt()),
                        daemonEntry.getHostname(),
                        daemonEntry.getDaemonAddress().getDaemonId()});
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
            String webAddress = "http://" + DaemonEntry.getDaemonAddress().getPeerAddress().getIp().getHostAddress() + ":" + webPort + "/" + DaemonEntry.getDaemonAddress().getPeerAddress().getId() + "/";
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
            outstandingTimeouts = new HashSet<UUID>();
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
