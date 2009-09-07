/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics.master.swing;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.jdesktop.application.Application.ExitListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.master.swing.exp.ExpEntry;
import se.sics.kompics.master.swing.exp.ExpPane;
import se.sics.kompics.master.swing.exp.ExpTreeModel;
import se.sics.kompics.master.swing.model.ExecEntry;
import se.sics.kompics.wan.job.ArtifactJob;
import se.sics.kompics.master.swing.model.NodeEntry;
import se.sics.kompics.master.swing.model.NodeEntry.ConnectionStatus;
import se.sics.kompics.master.swing.model.UserEntry;
import se.sics.kompics.master.swing.model.UserModel;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.mina.MinaNetwork;
import se.sics.kompics.network.mina.MinaNetworkInit;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.kompics.wan.config.MasterConfiguration;
import se.sics.kompics.wan.hosts.HostsPort;
import se.sics.kompics.wan.hosts.HostsXMLComponent;
import se.sics.kompics.wan.hosts.events.AddNodesRequest;
import se.sics.kompics.wan.hosts.events.AddNodesResponse;
import se.sics.kompics.wan.hosts.events.GetNodesRequest;
import se.sics.kompics.wan.hosts.events.GetNodesResponse;
import se.sics.kompics.wan.hosts.events.RemoveNodesRequest;
import se.sics.kompics.wan.hosts.events.RemoveNodesResponse;
import se.sics.kompics.wan.job.Job;
import se.sics.kompics.wan.job.JobExecRequest;
import se.sics.kompics.wan.job.JobExecResponse;
import se.sics.kompics.wan.job.JobLoadResponse;
import se.sics.kompics.wan.job.JobStopRequest;
import se.sics.kompics.wan.master.Master;
import se.sics.kompics.wan.master.MasterInit;
import se.sics.kompics.wan.master.MasterPort;
import se.sics.kompics.wan.master.events.ConnectedDaemonNotification;
import se.sics.kompics.wan.master.events.GetLoadedJobsResponse;
import se.sics.kompics.wan.master.events.InstallJobOnHostsRequest;
import se.sics.kompics.wan.master.events.InstallJobOnHostsResponse;
import se.sics.kompics.wan.master.events.JobsFound;
import se.sics.kompics.wan.plab.PlanetLabCredentials;
import se.sics.kompics.wan.services.ExperimentServicesComponent;
import se.sics.kompics.wan.services.ServicesPort;
import se.sics.kompics.wan.services.events.ActionProgressResponse;
import se.sics.kompics.wan.services.events.ActionSshTimeout;
import se.sics.kompics.wan.services.events.ExperimentServicesInit;
import se.sics.kompics.wan.services.events.GetDaemonLogsRequest;
import se.sics.kompics.wan.services.events.GetStatusRequest;
import se.sics.kompics.wan.services.events.InstallDaemonOnHostsRequest;
import se.sics.kompics.wan.services.events.InstallJavaOnHostsRequest;
import se.sics.kompics.wan.services.events.StartDaemonOnHostsRequest;
import se.sics.kompics.wan.services.events.StartDaemonOnHostsResponse;
import se.sics.kompics.wan.services.events.StopDaemonOnHostsRequest;
import se.sics.kompics.wan.services.events.StopDaemonOnHostsResponse;
import se.sics.kompics.wan.ssh.Credentials;
import se.sics.kompics.wan.ssh.ExperimentHost;
import se.sics.kompics.wan.ssh.Host;
import se.sics.kompics.wan.ssh.SshComponent;
import se.sics.kompics.wan.ssh.SshPort;
import se.sics.kompics.wan.ssh.events.SshHeartbeatResponse;
import se.sics.kompics.wan.ssh.scp.DownloadUploadMgr;
import se.sics.kompics.wan.ssh.scp.DownloadUploadPort;
import se.sics.kompics.wan.ssh.scp.ScpComponent;
import se.sics.kompics.wan.ssh.scp.ScpPort;
import se.sics.kompics.wan.util.PomUtils;

/**
 *
 * @author jdowling
 */
public class Client extends ComponentDefinition implements ExitListener {

    public static final int SSH_CONNECT_TIMEOUT = 300 * 1000;
    public static final int SSH_KEY_EXCHANGE_TIMEOUT = 15000;
    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    private Component xmlStore;
    private Component services;
    private Component ssh;
    private Component scp;
    private Component downloadUploadMgr;
    private Component timer;
    private Component master;
    private Component net;
    private UserModel userModel;
    private Credentials cred = null;
    private NodeListModel listModel = new NodeListModel();
    private Set<Job> loadedJobs = new LinkedHashSet<Job>();
    private Map<Integer, ExpPane> mapExperiments = new HashMap<Integer, ExpPane>();
    // TODO synchronize updates to ExpListModel instances by (1) client and (2) GUI
    // (expId, expModel)
    private Map<Integer, ExpTreeModel> mapExpModels = new ConcurrentHashMap<Integer, ExpTreeModel>();
    // (jobId, JobExecReqeuest)
    private Map<Integer, ExecEntry> mapExecModels = new ConcurrentHashMap<Integer, ExecEntry>();
    private Map<Integer, ArtifactJob> monitorJobLoading = new ConcurrentHashMap<Integer, ArtifactJob>();
    private Map<Integer, ArtifactJob> bootstrapJobLoading = new ConcurrentHashMap<Integer, ArtifactJob>();
    private String bootstrapServer;
    private String monitorServer;

    @Override
    public boolean canExit(EventObject event) {

        // say if able to exit now

        return true;
    }

    @Override
    public void willExit(EventObject event) {
        // cleanup before exiting
        // save user model
        // save node list model
    }

    public static class CleanupConnections extends Timeout {

        public CleanupConnections(ScheduleTimeout request) {
            super(request);
        }
    }

    public Client() {

        xmlStore = create(HostsXMLComponent.class);
        services = create(ExperimentServicesComponent.class);

        timer = create(JavaTimer.class);
        ssh = create(SshComponent.class);
        scp = create(ScpComponent.class);
        downloadUploadMgr = create(DownloadUploadMgr.class);

        master = create(Master.class);
        net = create(MinaNetwork.class);

        connect(ssh.getNegative(DownloadUploadPort.class), downloadUploadMgr.getPositive(DownloadUploadPort.class));
        connect(downloadUploadMgr.getNegative(ScpPort.class), scp.getPositive(ScpPort.class));
        connect(services.getNegative(SshPort.class),
                ssh.getPositive(SshPort.class));
        connect(services.getNegative(Timer.class), timer.getPositive(Timer.class));
        connect(master.getNegative(Network.class), net.getPositive(Network.class));
        connect(master.getNegative(Timer.class), timer.getPositive(Timer.class));

        subscribe(handleFault, xmlStore.getControl());
        subscribe(handleGetNodesResponse, xmlStore.getPositive(HostsPort.class));
        subscribe(handleAddNodesResponse, xmlStore.getPositive(HostsPort.class));
        subscribe(handleRemoveNodesResponse, xmlStore.getPositive(HostsPort.class));

        subscribe(handleActionProgressResponse, services.getPositive(ServicesPort.class));
        subscribe(handleStopDaemonOnHostsResponse, services.getPositive(ServicesPort.class));
        subscribe(handleStartDaemonOnHostsResponse, services.getPositive(ServicesPort.class));

        subscribe(handleGetLoadedJobsResponse, master.getPositive(MasterPort.class));
        subscribe(handleJobsFound, master.getPositive(MasterPort.class));
        subscribe(handleInstallJobOnHostsResponse, master.getPositive(MasterPort.class));
        subscribe(handleJobsFound, master.getPositive(MasterPort.class));
        subscribe(handleJobExecResponse, master.getPositive(MasterPort.class));
        subscribe(handleConnectedDaemonsNotification, master.getPositive(MasterPort.class));

        subscribe(handleActionSshTimeout, services.getPositive(ServicesPort.class));
        subscribe(handleStart, control);

        subscribe(handleCleanupConnections, timer.getPositive(Timer.class));
        subscribe(handleSshHeartbeatResponse, ssh.getPositive(SshPort.class));

        ComponentRegistry.registerComponent(this.getClass(), this, this.getClass().getCanonicalName());

        createUserModel();

        trigger(new ExperimentServicesInit(), services.getControl());
        MasterInit init = new MasterInit(MasterConfiguration.getMasterAddress(),
                MasterConfiguration.getBootConfiguration());
        // , MasterConfiguration.getMonitorConfiguration()
        trigger(init, master.getControl());
        trigger(new MinaNetworkInit(MasterConfiguration.getMasterAddress()), net.getControl());

        logger.info("Master listening on: {}", MasterConfiguration.getMasterAddress().toString());

    }

    private void createUserModel() {
        userModel = new UserModel();
        try {
            userModel.load(MasterConfiguration.USER_FILE);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, "user model file not found");
        }
        UserEntry userEntry = userModel.getUserEntry();
        if (userEntry != null) {
            cred = userEntry.getCredentials();
        }
        userEntry.addPropertyChangeListener(listModel);
    }
    private Handler<ActionSshTimeout> handleActionSshTimeout = new Handler<ActionSshTimeout>() {

        public void handle(ActionSshTimeout event) {

            Host h = event.getHost();
            logger.warn("Ssh connection timeout for host: {}", h.getHostname());

            NodeEntry node = new NodeEntry(h);
            node.setConnectionStatus(ConnectionStatus.NOT_CONTACTABLE);
            node.setMsg("Ssh connection timeout for this host.");
            listModel.addNodeEntry(node);

            // XXX write to an error message area somewhere
            // when this node is selected in the area msg, we write "ssh timeout, node not contactable"

        }
    };
    private Handler<StopDaemonOnHostsResponse> handleStopDaemonOnHostsResponse = new Handler<StopDaemonOnHostsResponse>() {

        public void handle(StopDaemonOnHostsResponse event) {
            logger.info("Simple client received stop daemon response. Updating status.");
            Set<String> hosts = event.getHosts();
            trigger(new GetStatusRequest(cred, hosts), services.getPositive(ServicesPort.class));
        }
    };
    private Handler<StartDaemonOnHostsResponse> handleStartDaemonOnHostsResponse = new Handler<StartDaemonOnHostsResponse>() {

        public void handle(StartDaemonOnHostsResponse event) {
            logger.info("Simple client received Start daemon response. Updating status.");
            Set<String> hosts = event.getHosts();
            trigger(new GetStatusRequest(cred, hosts), services.getPositive(ServicesPort.class));
        }
    };
    public Handler<CleanupConnections> handleCleanupConnections = new Handler<CleanupConnections>() {

        public void handle(CleanupConnections event) {
            // XXX remove connections that haven't sent a reply for N heartbeats
//			for (Host host : connectedHosts) {
//				int sId = host.getSessionId();
//				Boolean recvdHB = mapConnectedHosts.get(sId);
//				if (recvdHB == null) {
//					host.incHearbeatTimeout();
//				} else {
//					if (recvdHB == true) {
//						host.zeroHearbeatTimeout();
//					}
//				}
//				if (host.getHeartbeatTimeout() > 3) {
//					connectedHosts.remove(host);
//				} else {
//					// ping the remaining open ssh connections
//					trigger(new SshHeartbeatRequest(sId), ssh
//							.getPositive(SshPort.class));
//				}
//			}
//			mapConnectedHosts.clear();
        }
    };
    public Handler<SshHeartbeatResponse> handleSshHeartbeatResponse = new Handler<SshHeartbeatResponse>() {

        public void handle(SshHeartbeatResponse event) {
//			mapConnectedHosts.put(event.getSessionId(), event.isStatus());
        }
    };
    private Handler<ActionProgressResponse> handleActionProgressResponse = new Handler<ActionProgressResponse>() {

        public void handle(ActionProgressResponse event) {

            logger.info("Received node status update at SC.");

            Host host = event.getHost();
            ExperimentServicesComponent.ServicesStatus status = event.getStatus();

            logger.info("Daemon is {}", status.getDaemonRunning());

            NodeEntry entry = listModel.getNodeEntry(host);
            if (entry != null) {
                entry.setDaemonInstallStatus(status.getDaemon());
                entry.setJavaInstallStatus(status.getJava());
                entry.setDaemonRunningStatus(status.getDaemonRunning());
                if (status.isConnected() == true) {
                    entry.setConnectionStatus(ConnectionStatus.CONNECTED);
                    entry.setMsg("Connected");
                } else {
                    entry.setConnectionStatus(ConnectionStatus.NOT_CONTACTABLE);
                    entry.setMsg("Ssh Connection/Authentication problem for host");
                }
                listModel.addNodeEntry(entry);
            } else {
                logger.warn("Could not find NodeEntry for host: {}", host.getHostname());
            }
        }
    };

    public NodeListModel getListModel() {
        return listModel;
    }

    public UserEntry getUserEntry() {
        return userModel.getUserEntry();
    }
//    private NodeEntry getNodeEntry(Host host) {
//        NodeEntry node = null;
//        for (NodeEntry entry : nodeModel.getNodeEntries()) {
//            if (entry.getHost().equals(host) == true) {
//                return entry;
//            }
//        }
//        return node;
//    }
    private Handler<GetNodesResponse> handleGetNodesResponse = new Handler<GetNodesResponse>() {

        public void handle(GetNodesResponse event) {

            logger.info("Received {} hosts from planetlab not in slice", event.getHosts().size());

            Set<Host> hosts = event.getHosts();

            for (Host h : hosts) {
                listModel.addHost(h);
            }

//            nodeModel = new NodeModel(hosts);
//                nodesAvailable.release();
//                for (Host h : hosts) {
//                    NodeEntry n = new NodeEntry(h);
//                    nodeModel.addNodeEntry(n);
//                }
        }
    };
    private Handler<AddNodesResponse> handleAddNodesResponse = new Handler<AddNodesResponse>() {

        public void handle(AddNodesResponse event) {

            boolean success = event.getHostStatus();

        }
    };
    private Handler<RemoveNodesResponse> handleRemoveNodesResponse = new Handler<RemoveNodesResponse>() {

        public void handle(RemoveNodesResponse event) {
        }
    };
    private Handler<JobExecResponse> handleJobExecResponse = new Handler<JobExecResponse>() {

        public void handle(JobExecResponse event) {

            // TODO
            logger.info("Received jobexec response for job {} , response : ", event.getJobId(), event.getStatus());

            ExecEntry entry = mapExecModels.get(event.getJobId());
            entry.setResult("Job Id: " + event.getJobId() + " - " + event.getStatus().toString());
        }
    };
    private Handler<JobsFound> handleJobsFound = new Handler<JobsFound>() {

        public void handle(JobsFound event) {

            logger.info("Found {} jobs at {}", event.getJobs().size(), event.getAddr().toString());
            loadedJobs.addAll(event.getJobs());

            Address addr = event.getAddr();
            Set<Job> jobs = event.getJobs();
            for (Job j : jobs) {
                int jobId = j.getId();
                ExpPane expPane = mapExperiments.get(jobId);
                if (expPane != null) {
                    expPane.updateNodeState(addr.getIp(), jobId, ExpEntry.ExperimentStatus.LOADED);
                } else {
                    logger.debug("Found job:  {}. Could not find experiment for it.", jobId);
                }
            }
        }
    };
    private Handler<ConnectedDaemonNotification> handleConnectedDaemonsNotification = new Handler<ConnectedDaemonNotification>() {

        public void handle(ConnectedDaemonNotification event) {

            Host host = new ExperimentHost(event.getDaemonHost());

            logger.info("Daemon connected {}", host);

            NodeEntry entry = listModel.getNodeEntry(host);
            if (entry != null) {
                entry.setDaemonConnectionStatus(NodeEntry.ConnectionStatus.CONNECTED);
            } else {
                logger.warn("Could not find NodeEntry for host: {}", host.getHostname());
            }
        }
    };
    private Handler<InstallJobOnHostsResponse> handleInstallJobOnHostsResponse = new Handler<InstallJobOnHostsResponse>() {

        public void handle(InstallJobOnHostsResponse event) {

            JobLoadResponse.Status status = event.getStatus();
            String msg = event.getMsg();

            int jobId = event.getJobId();
            InetAddress ip = event.getIp();
            ExpPane expPane = mapExperiments.get(jobId);

            ExpEntry.ExperimentStatus expStatus = ExpEntry.ExperimentStatus.NOT_LOADED;

            if (status == JobLoadResponse.Status.ASSEMBLED) {
                expStatus = ExpEntry.ExperimentStatus.LOADED;
            }
            if (status == JobLoadResponse.Status.FAIL) {
                expStatus = ExpEntry.ExperimentStatus.ERROR;
            }

            if (expPane != null) {
                expPane.updateNodeState(ip, jobId, expStatus);
                logger.info("Job loading was {} . Msg was: {}", expStatus, msg);
            } else {
                logger.warn("Could not find experiment object for job {}", jobId);
            }

            ArtifactJob artifact = monitorJobLoading.get(event.getJobId());
            if (artifact != null) {
                ExecEntry execEntry = mapExecModels.get(event.getJobId());
                String mPort = artifact.getPort();
                String mWebPort = artifact.getWebPort();
                try {
                    // sleep for a few seconds to wait for artifact to assemble
                    Thread.currentThread().sleep(8 * 1000);
                } catch (InterruptedException ex) {
                    java.util.logging.Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
                sendJob(ip.getHostAddress(), artifact, bootstrapServer, "7001", "7000",
                        ip.getHostAddress(), mPort, mWebPort, 1, execEntry);
            }
            ArtifactJob a2 = bootstrapJobLoading.get(event.getJobId());
            if (a2 != null) {
                ExecEntry execEntry = mapExecModels.get(event.getJobId());
                String bPort = a2.getPort();
                String bWebPort = a2.getWebPort();
                try {
                    // sleep for a few seconds to wait for artifact to assemble
                    Thread.currentThread().sleep(8 * 1000);
                } catch (InterruptedException ex) {
                    java.util.logging.Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
                sendJob(ip.getHostAddress(), a2, ip.getHostAddress(), bPort, bWebPort,
                        monitorServer, "7003", "7002", 1, execEntry);
            }

        }
    };
    private Handler<GetLoadedJobsResponse> handleGetLoadedJobsResponse = new Handler<GetLoadedJobsResponse>() {

        public void handle(GetLoadedJobsResponse event) {
            loadedJobs.clear();
            loadedJobs.addAll(event.getJobs());
        }
    };

    public List<Job> getJobs() {
        return new ArrayList<Job>(loadedJobs);
    }

    public void installJobOnHosts(String groupId, String artifactId, String version, String mainClass,
            List<String> args, String repoId,
            String repoUrl, boolean isHideMavenOutput, Set<Host> hosts) {

        InstallJobOnHostsRequest job = new InstallJobOnHostsRequest(groupId, artifactId, version,
                mainClass, args, repoId, repoUrl, isHideMavenOutput, hosts);
        trigger(job, master.getPositive(MasterPort.class));
    }

    private int getJobId(String groupId, String artifactId, String version) {
        return PomUtils.generateJobId(groupId, artifactId, version);
    }

//    public void startJobOnHosts(String groupId, String artifactId, String version,
//            int numPeers) {
//        int jobId = getJobId(groupId, artifactId, version);
//        StartJobOnHostsRequest req = new StartJobOnHostsRequest(jobId, numPeers);
//        trigger(req, master.getPositive(MasterPort.class));
//    }
    public void stopJob(String hostname, String groupId, String artifactId, String version) {
        int jobId = getJobId(groupId, artifactId, version);
        JobStopRequest req = new JobStopRequest(jobId, hostname);
        trigger(req, master.getPositive(MasterPort.class));
    }

    public Credentials getCredentials() {
        return cred;
    }

    public void setCredentials(Credentials cred) {
        this.cred = cred;

//        UserEntry entry = userModel.getUserEntry();
//        if (entry == null) {
//            entry = new UserEntry();
//        }

        UserEntry entry = userModel.getUserEntry();
        entry.setSshLoginName(cred.getSshLoginName());
        entry.setSshPassword(cred.getSshPassword());
        entry.setSshKeyFilename(cred.getKeyPath());
        entry.setSshKeyFilePassword(cred.getKeyFilePassword());
        if (cred instanceof PlanetLabCredentials) {
            entry.setSlice(((PlanetLabCredentials) cred).getSlice());
        } else {
            entry.setSlice("");
        }
        userModel.setUserEntry(entry);

        entry.addPropertyChangeListener(listModel);


        try {
            userModel.save(MasterConfiguration.USER_FILE);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

//    public List<NodeEntry> getNodes() throws InterruptedException {
//        List<NodeEntry> entries = new ArrayList<NodeEntry>();
//        logger.info("Waiting for hosts to be loaded...");
//        return entries;
//    }
    public void addNodes(Set<Host> hosts) {


        for (Host h : hosts) {
            listModel.addHost(h);
        }

        trigger(new AddNodesRequest(hosts), xmlStore.getPositive(HostsPort.class));
        // update their status
        updateNodesStatus(hosts);
    }

    public void removeNodes(Set<NodeEntry> nodes) {
        logger.info("Removing hosts");

        Set<Host> hosts = new HashSet<Host>();
        for (NodeEntry node : nodes) {
            hosts.add(node.getHost());
            listModel.removeNode(node);
//            NodeEntry ne = new NodeEntry(h);
//            int index = listController.getEntries().size();
//            List<NodeEntry> newEntries = Arrays.asList(ne);
//            listController.getEntries().removeAll(newEntries);
//            listController.setSelection();
        }
        // persist changes
        trigger(new RemoveNodesRequest(hosts), xmlStore.getPositive(HostsPort.class));
    }

    private Set<String> nodesToHostnames(Set<NodeEntry> nodes) {
        Set<String> hosts = new HashSet<String>();
        for (NodeEntry n : nodes) {
            hosts.add(n.getHostname());
        }
        return hosts;
    }

    public void installDaemonOnNodes(Set<NodeEntry> nodes, boolean force) {
        logger.info("Installing daemon on hosts");
        Set<String> hosts = nodesToHostnames(nodes);
        trigger(new InstallDaemonOnHostsRequest(cred, hosts, force), services.getPositive(ServicesPort.class));
    }

    public void refreshNodesStatus(Set<NodeEntry> nodes) {
        logger.info("Getting node status");
        Set<String> hosts = nodesToHostnames(nodes);
        trigger(new GetStatusRequest(cred, hosts), services.getPositive(ServicesPort.class));
    }

    public Set<Host> getHosts() {
        Set<Host> hosts = nodesToHosts(this.listModel.getNodes());
        return hosts;
    }

    private Set<Host> nodesToHosts(Set<NodeEntry> nodes) {
        Set<Host> hosts = new HashSet<Host>();
        for (NodeEntry node : nodes) {
            hosts.add(node.getHost());
        }
        return hosts;
    }

    private void updateNodesStatus(Set<Host> nodes) {
        logger.debug("Updating node status");
        Set<String> hosts = new HashSet<String>();
        for (Host h : nodes) {
            hosts.add(h.getHostname());
        }
        trigger(new GetStatusRequest(cred, hosts), services.getPositive(ServicesPort.class));
    }

//    public void connectHosts(Set<NodeEntry> nodes) {
//        for (NodeEntry node : nodes) {
//            Host host = node.getHost();
//
//               UUID requestId = UUID.randomUUID();
//            trigger(new SshConnectRequest(cred, requestId, host), sshComponent.getPositive(SshPort.class));
//        }
//    }
    public void installJava(Set<NodeEntry> nodes, boolean force) {
        logger.debug("Installing java on nodes");
        Set<String> hosts = nodesToHostnames(nodes);
        trigger(new InstallJavaOnHostsRequest(cred, hosts, force), services.getPositive(ServicesPort.class));
    }

    public void daemonLogs(Set<NodeEntry> nodes) {
        logger.debug("Getting daemon logs from nodes");
        Set<String> hosts = nodesToHostnames(nodes);
        trigger(new GetDaemonLogsRequest(cred, hosts), services.getPositive(ServicesPort.class));
    }

    public void startDaemon(Set<NodeEntry> nodes) {
        logger.debug("Starting daemon on nodes");
        Set<String> hosts = nodesToHostnames(nodes);
        trigger(new StartDaemonOnHostsRequest(cred, hosts), services.getPositive(ServicesPort.class));
    }

    public void stopDaemon(Set<NodeEntry> nodes) {
        logger.debug("Stopping daemon on nodes");
        Set<String> hosts = nodesToHostnames(nodes);
        trigger(new StopDaemonOnHostsRequest(cred, hosts), services.getPositive(ServicesPort.class));
    }
    Handler<Fault> handleFault = new Handler<Fault>() {

        public void handle(Fault fault) {
            fault.getFault().printStackTrace(System.err);
        }
    };
    Handler<Start> handleStart = new Handler<Start>() {

        public void handle(Start event) {
            trigger(new GetNodesRequest(), xmlStore.getPositive(HostsPort.class));
        }
    };

    public ExpTreeModel getTreeModel(int id) {
        return mapExpModels.get(id);
    }

    public ExpTreeModel putTreeModel(int expId, ExpTreeModel model) {
        return mapExpModels.put(expId, model);
    }

    public void shutdownAllConnectedDaemons() {
        // TODO
//        trigger(ShutdownDaemonRequest(), maven.get);
    }

    public void registerExperiment(int jobId, ExpPane expPane) {
        mapExperiments.put(jobId, expPane);
    }

    public void startMonitor(ArtifactJob artifact, ExecEntry execEntry) {


        int jobId = getJobId(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());

        monitorJobLoading.put(jobId, artifact);
        this.mapExecModels.put(jobId, execEntry);

        trigger(new InstallJobOnHostsRequest(artifact, monitorServer), master.getPositive(MasterPort.class));
    }

    public void startBootstrap(ArtifactJob artifact, ExecEntry execEntry) {
        int jobId = getJobId(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
        bootstrapJobLoading.put(jobId, artifact);
        this.mapExecModels.put(jobId, execEntry);

        trigger(new InstallJobOnHostsRequest(artifact, bootstrapServer), master.getPositive(MasterPort.class));
    }

    private void sendJob(String host, ArtifactJob artifact,
            String bootHost, String bootPort, String bootWebPort,
            String monitorHost, String monitorPort, String monitorWebPort,
            int numPeers, ExecEntry execEntry) {
        int jobId = getJobId(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());

        this.mapExecModels.put(jobId, execEntry);

        String[] args = artifact.getArgs().split(" ");
        List<String> listArgs = Arrays.asList(args);
        JobExecRequest req = new JobExecRequest(
                host, numPeers,
                artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getVersion(),
                artifact.getRepoId(), artifact.getRepoUrl(),
                artifact.getMainClass(), listArgs,
                Integer.parseInt(artifact.getPort()), Integer.parseInt(artifact.getWebPort()),
                bootHost, Integer.parseInt(bootPort), Integer.parseInt(bootWebPort),
                monitorHost, Integer.parseInt(monitorPort), Integer.parseInt(monitorWebPort));
        trigger(req, master.getPositive(MasterPort.class));


    }

    public void startJob(String host, ArtifactJob artifact, ArtifactJob bootstrap,
            ArtifactJob monitor,
            int numPeers, ExecEntry execEntry) {


        sendJob(host, artifact, bootstrapServer, bootstrap.getPort(), bootstrap.getWebPort(),
                monitorServer, monitor.getPort(), monitor.getWebPort(), numPeers, execEntry);

        if (bootstrapServer.compareTo("") == 0 || monitorServer.compareTo("") == 0) {
            logger.warn("You must start the bootstrap server and monitor server before starting a job!");

            execEntry.setCommand("You must start the bootstrap server and monitor server before starting a job!");
        }
    }

    public void setBootstrapServer(String boot) {
        this.bootstrapServer = boot;
    }

    public void setMonitorServer(String monitor) {
        this.monitorServer = monitor;
    }

    public String getBootstrapServer() {
        return bootstrapServer;
    }

    public String getMonitorServer() {
        return monitorServer;
    }
}
