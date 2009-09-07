package se.sics.kompics.wan.services;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.config.MasterConfiguration;
import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.plab.PLabHost;
import se.sics.kompics.wan.services.events.ActionProgressResponse;
import se.sics.kompics.wan.services.events.ActionRequest;
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
import se.sics.kompics.wan.ssh.SshPort;
import se.sics.kompics.wan.ssh.events.DownloadFileRequest;
import se.sics.kompics.wan.ssh.events.SshCommandRequest;
import se.sics.kompics.wan.ssh.events.SshCommandResponse;
import se.sics.kompics.wan.ssh.events.SshConnectRequest;
import se.sics.kompics.wan.ssh.events.SshConnectResponse;
import se.sics.kompics.wan.ssh.events.SshConnectionTimeout;
import se.sics.kompics.wan.ssh.events.UploadFileRequest;
import se.sics.kompics.wan.ssh.events.UploadFileResponse;
import se.sics.kompics.wan.util.LocalNetworkConfiguration;

/**
 * The PLabServices component is used to provide services required before
 * starting experiments, including:
 * <ul>
 * <li>Install daemons on a set of hosts</li>
 * <li>Install java on a set of hosts</li>
 * <li>Start experiment services, including bootstrap server, monitor server,
 * and logging server.</li>
 * </ul>
 * 
 * 
 * @author jdowling
 * 
 */
public class ExperimentServicesComponent extends ComponentDefinition {

    private final Logger logger = LoggerFactory.getLogger(ExperimentServicesComponent.class);

    public enum Action {

        GET_STATUS, INSTALL_JAVA, INSTALL_DAEMON, START_DAEMON, STOP_DAEMON, GET_DAEMON_LOGS
    }

    public static class ServicesStatus {

        public enum Installation {

            NOT_INSTALLED, CONNECTING, INSTALLING, INSTALLED
        };

        public enum Program {

            RUNNING, STOPPED
        };
        private Installation java;
        private Installation daemon;
        private Program daemonRunning;
        private boolean connected;

        public ServicesStatus(boolean connection) {
            this.java = Installation.NOT_INSTALLED;
            this.daemon = Installation.NOT_INSTALLED;
            this.daemonRunning = Program.STOPPED;
            this.connected = connection;
        }

        public ServicesStatus(boolean connection, Installation java, Installation daemon,
                Program daemonService) {
            super();
            this.connected = connection;
            this.java = java;
            this.daemon = daemon;
            this.daemonRunning = daemonService;
        }

        public Installation getDaemon() {
            return daemon;
        }

        public Installation getJava() {
            return java;
        }

        public void setDaemon(Installation daemon) {
            this.daemon = daemon;
        }

        public void setJava(Installation java) {
            this.java = java;
        }

        public Program getDaemonRunning() {
            return daemonRunning;
        }

        public void setDaemonRunning(Program daemonRunning) {
            this.daemonRunning = daemonRunning;
        }

        public boolean isConnected() {
            return connected;
        }

        public void setConnected(boolean connection) {
            this.connected = connection;
        }
    }
    private Negative<ServicesPort> servicesPort = negative(ServicesPort.class);
    private Positive<SshPort> sshPort = positive(SshPort.class);
    private Positive<Timer> timerPort = positive(Timer.class);
    // private Installer javaInstaller = new Installer(
    // PlanetLabConfiguration.DEFAULT_NUM_ACTIVE_TRANSFERS, new File(
    // PlanetLabConfiguration.DEFAULT_JAVA_INSTALLATION_FILE),
    // "~/java/");
    // private Credentials cred;
    /**
     * used to map SshConnectionRequests to their corresponding ActionRequests
     */
    private Map<UUID, ActionRequest> connectionRequests = new HashMap<UUID, ActionRequest>();
    /**
     * <installationId,numHostsToPerformInstallation> pairs
     */
    private Map<ActionRequest, Integer> actionRequests = new HashMap<ActionRequest, Integer>();
    /**
     * Maps sessionIds to hostnames
     */
    private Map<Integer, Host> sessionHosts = new HashMap<Integer, Host>();
    /**
     * Cached status of services on hosts
     */
    private Map<Host, ServicesStatus> hostStatus = new HashMap<Host, ServicesStatus>();
    private final HashSet<UUID> outstandingTimeouts = new HashSet<UUID>();

    public ExperimentServicesComponent() {



        subscribe(handleInstallDaemonOnHostsRequest, servicesPort);
        subscribe(handleInstallJavaOnHostsRequest, servicesPort);
        subscribe(handleStartDaemonOnHostsRequest, servicesPort);
        subscribe(handleStopDaemonOnHostsRequest, servicesPort);
        subscribe(handleGetStatusRequest, servicesPort);

        subscribe(handleGetDaemonLogsRequest, servicesPort);

        subscribe(handleInit, control);

        subscribe(handleUploadFileResponse, sshPort);
        subscribe(handleConnectResponse, sshPort);
        subscribe(handleCommandResponse, sshPort);


        subscribe(handleSshConnectionTimeout, timerPort);
    }
    public Handler<ExperimentServicesInit> handleInit = new Handler<ExperimentServicesInit>() {

        public void handle(ExperimentServicesInit event) {
            logger.info("Initializing ExperimentServices...");
        }
    };
    public Handler<SshConnectionTimeout> handleSshConnectionTimeout =
            new Handler<SshConnectionTimeout>() {

                public void handle(SshConnectionTimeout event) {
                    UUID requestId = event.getTimeoutId();
                    if (!outstandingTimeouts.contains(requestId)) {
                        return;
                    }
                    outstandingTimeouts.remove(event.getTimeoutId());

                    logger.warn("Ssh connect timeout");

                    ActionRequest request = connectionRequests.get(requestId);
                    receivedActionRequest(request);
                    String h = event.getHost();
                    Host host = new ExperimentHost(h);
                    trigger(new ActionSshTimeout(request, host), servicesPort);
//                    ServicesStatus status = new ServicesStatus(false);
//                    sendActionResponse(host, request.getAction(), status, false);

                }
            };

    private void receivedActionRequest(ActionRequest request) {
        int numOutstandingReplies = actionRequests.get(request);
        numOutstandingReplies--;

        if (numOutstandingReplies == 0) {
            actionRequests.remove(request);
        } else {
            actionRequests.put(request, numOutstandingReplies);
        }
    }

    private void executeAction(Credentials cred, ActionRequest event,
            Set<String> hosts, Action action) {
        if (actionRequests.get(event) != null) {
            logger.warn("Duplicate request received");
            return;
        }

        int numOutstandingConnectionRequests = hosts.size();
        for (String host : hosts) {
            Host h = new ExperimentHost(host);

            boolean requestSshConnection = true;
            if (hostStatus.containsKey(h) == true) { // check if already in the cache
                ServicesStatus status = hostStatus.get(h);
                if (event instanceof InstallJavaOnHostsRequest) {
                    if (status.getJava() == ServicesStatus.Installation.INSTALLED && ((InstallJavaOnHostsRequest) event).isForce() == false) {
                        sendActionResponse(h, action, status, true);
                        requestSshConnection = false;
                        numOutstandingConnectionRequests--;
                    }
                } else if (event instanceof InstallDaemonOnHostsRequest) {
                    if (status.getDaemon() == ServicesStatus.Installation.INSTALLED && ((InstallDaemonOnHostsRequest) event).isForce() == false) {
                        sendActionResponse(h, action, status, true);
                        requestSshConnection = false;
                        numOutstandingConnectionRequests--;
                    }
                }
            } else {
                hostStatus.put(h, new ServicesStatus(false));
            }

            if (requestSshConnection == true) {

                ScheduleTimeout st =
                        new ScheduleTimeout(SshConnectionTimeout.SSH_CONNECT_TIMEOUT);
                SshConnectionTimeout connectTimeout = new SshConnectionTimeout(st, host);
                st.setTimeoutEvent(connectTimeout);

                UUID requestId = connectTimeout.getTimeoutId();
                outstandingTimeouts.add(requestId);


                trigger(new SshConnectRequest(cred, requestId, h), sshPort);
                connectionRequests.put(requestId, event);

                trigger(st, timerPort);
            }
        }

        if (numOutstandingConnectionRequests > 0) {
            actionRequests.put(event, numOutstandingConnectionRequests);
        } else {
            actionRequests.remove(event);
        }
    }
    private Handler<InstallJavaOnHostsRequest> handleInstallJavaOnHostsRequest = new Handler<InstallJavaOnHostsRequest>() {

        public void handle(InstallJavaOnHostsRequest event) {

            Set<String> hosts = event.getHosts();
            executeAction(event.getCred(), event, hosts, Action.INSTALL_JAVA);
        }
    };
    private Handler<StopDaemonOnHostsRequest> handleStopDaemonOnHostsRequest =
            new Handler<StopDaemonOnHostsRequest>() {

                public void handle(StopDaemonOnHostsRequest event) {

                    Set<String> hosts = event.getHosts();
                    executeAction(event.getCred(), event, hosts, Action.START_DAEMON);
                }
            };
    private Handler<StartDaemonOnHostsRequest> handleStartDaemonOnHostsRequest = new Handler<StartDaemonOnHostsRequest>() {

        public void handle(StartDaemonOnHostsRequest event) {

            Set<String> hosts = event.getHosts();
            executeAction(event.getCred(), event, hosts, Action.START_DAEMON);
        }
    };
    private Handler<InstallDaemonOnHostsRequest> handleInstallDaemonOnHostsRequest = new Handler<InstallDaemonOnHostsRequest>() {

        public void handle(InstallDaemonOnHostsRequest event) {
            logger.info("InstallDaemonOnHostsRequest received");
            Set<String> hosts = event.getHosts();
            executeAction(event.getCred(), event, hosts, Action.INSTALL_DAEMON);
        }
    };
    private Handler<GetDaemonLogsRequest> handleGetDaemonLogsRequest = new Handler<GetDaemonLogsRequest>() {

        public void handle(GetDaemonLogsRequest event) {
            logger.info("GetDaemonLogsRequest received");
            Set<String> hosts = event.getHosts();
            executeAction(event.getCred(), event, hosts, Action.GET_DAEMON_LOGS);
        }
    };
    private Handler<GetStatusRequest> handleGetStatusRequest = new Handler<GetStatusRequest>() {

        public void handle(GetStatusRequest event) {
            logger.info("GetStatusRequest received");
            Set<String> hosts = event.getHosts();
            executeAction(event.getCred(), event, hosts, Action.GET_STATUS);
        }
    };
    private Handler<SshConnectResponse> handleConnectResponse = new Handler<SshConnectResponse>() {

        public void handle(SshConnectResponse event) {
            logger.info("SshConnectResponse received for sessionId {}", event.getSessionId());
            UUID requestId = event.getRequestId();

            if (outstandingTimeouts.contains(requestId)) {
                CancelTimeout ct = new CancelTimeout(requestId);
                trigger(ct, timerPort);
                outstandingTimeouts.remove(requestId);
            } else {
                return; // remove duplicates - the request has been cancelled/retried
            }

            ActionRequest request = connectionRequests.get(requestId);

            receivedActionRequest(request);

            int sessionId = event.getSessionId();

            if (sessionId != -1) {

                sessionHosts.put(sessionId, event.getHost());
                // Update sessionHosts to include SessionId in the host object

                if (request instanceof InstallJavaOnHostsRequest) {
                    installJava(requestId, sessionId);

                } else if (request instanceof InstallDaemonOnHostsRequest) {
                    trigger(new UploadFileRequest(requestId, sessionId, new File(
                            PlanetLabConfiguration.DEFAULT_DAEMON_JAR_FILE),
                            "~/.kompics/", true, 500 * 1000, true), sshPort);

                } else if (request instanceof StartDaemonOnHostsRequest) {
                    // XXX this is a hack that starts the daemon running no hangup,
                    // and then waits a couple of seconds to read its output
                    // It's very brittle - if daemon.log file is changed, it breaks

                    String commandStr = "echo \\\"" + StartDaemonOnHostsRequest.class.getCanonicalName() + "\\\" ; " + "echo \\\"" + requestId.toString() + "\\\" ; " + " PID=$(cat /home/$USER/.kompics/daemon.pid) ; " //  -a /proc/${PID}/exe -ef /usr/bin/program
                            + " if [ -e /proc/${PID}/exe ] ; then " + " echo \\\"Daemon already running with PID= $PID.\\\" ; " + " else " //                        + " if [ `ps -ef | grep " +
                            + " `nohup java -jar /home/$USER/.kompics/" + PlanetLabConfiguration.DAEMON_JAR_FILE
                            + " -master " + PlanetLabConfiguration.getMasterAddress().toString()
                            + " &> /home/$USER/.kompics/daemon.log & ` ; "
//                            + " > /home/$USER/.kompics/daemon.log 2> /home/$USER/.kompics/daemon.err < /dev/null & ` ; "
                            + " sleep 1 ; "
                            + " PID=`ps aux | awk '/" + PlanetLabConfiguration.DAEMON_JAR_FILE + "/ && !/awk/ { print $2 }'` ; echo $PID > /home/$USER/.kompics/daemon.pid ; " //                        + " head -1 /home/$USER/.kompics/daemon.log ; "
                            + " fi ";

                    sendSshCommand(requestId, sessionId, commandStr);
                } else if (request instanceof StopDaemonOnHostsRequest) {

                    String commandStr = "echo \\\"" + StopDaemonOnHostsRequest.class.getCanonicalName() + "\\\" ; " + "echo \\\"" + requestId.toString() + "\\\" ; " + " PID=`cat /home/$USER/.kompics/daemon.pid` ; " + " kill -9 $PID ; ";
//                        + " if [ -e /proc/${PID} -a /proc/${PID}/exe -ef /usr/bin/program ]; then "

                    sendSshCommand(requestId, sessionId, commandStr);

                } else if (request instanceof GetStatusRequest) {

                    String commandStr = "echo \\\"" + GetStatusRequest.class.getCanonicalName() + "\\\" ; " + "echo \\\"" + requestId.toString() + "\\\" ; " + " if [ -f  /home/$USER/.kompics/" + PlanetLabConfiguration.DAEMON_JAR_FILE + " ] ; then" + " echo \\\"daemon=true\\\" ; else echo \\\"daemon=false\\\" ; fi ; " // + " echo \\\"java = \\\" ; java -version ;"
                            + " IS_JAVA=`which java` ; echo \\\"java=${IS_JAVA:(-4)}\\\" ;" 
                            + " if [ `netstat -lpe 2> /dev/null | grep java" 
                            + " | grep " + Configuration.DEFAULT_DAEMON_PORT + " | wc -l`"
                            + " -gt 1  ] ; then echo \\\"daemonService=true\\\" ; "
//                            + " if [ `ps -ef | grep " + PlanetLabConfiguration.DAEMON_JAR_FILE
//                            + " -c` -gt 1  ] ; then echo \\\"daemonService=true\\\" ; "
                            + "else echo \\\"daemonService=false\\\" ; fi ;" + " [ -a /home/$USER/.kompics ] || mkdir /home/$USER/.kompics ;";

                    sendSshCommand(requestId, sessionId, commandStr);

                } else if (request instanceof GetDaemonLogsRequest) {
                    GetDaemonLogsRequest dReq = (GetDaemonLogsRequest) request;

                    trigger(new DownloadFileRequest(requestId, sessionId, "/home/"
                            + event.getCred().getSshLoginName() + "/.kompics/daemon.log",
                            Configuration.DAEMON_LOGS_DIR + event.getHost().getHostname()),
                            sshPort);
                }
                else {
                    throw new IllegalStateException(
                            "Undefined action  request type.");
                }



                if (actionRequests.containsKey(request) == false) {
                    // don't need to store connection requests any more - got all
                    // connection responses
//				connectionRequests.remove(request);
                    // if (request instanceof InstallJavaOnHostsRequest) {
                    //
                    // } else if (request instanceof InstallDaemonOnHostsRequest) {
                    //
                    // // trigger(new InstallDaemonOnHostsResponse(request, hosts,
                    // // status), servicesPort);
                    // } else if (request instanceof StartDaemonOnHostsRequest) {
                    //
                    // } else {
                    // throw new IllegalStateException(
                    // "Undefined action request type.");
                    // }
                }
            } else // sessionId == -1
            {
                ServicesStatus status = new ServicesStatus(false);
//                hostStatus.put(event.getHost(), status);
                connectionRequests.remove(requestId);
                sendActionResponse(event.getHost(), request.getAction(), status, false);
            }
        }
    };

    private void installJava(UUID requestId, int sessionId) {
        //				String java = "jre-6u15-linux-i586.bin";
        String java = "jre.tgz";
        String installDir = "jre1.6.0_15";
        String md5sum = "d6e0603d690dd80a33cc150d5d7f866a";
//      String md5sum = "2fc2c730529932feaac401476afaa94f";

        String javaBash = "export JAVA_HOME=/home/\\$USER/" + installDir;
        String pathBash = "export PATH=\\$PATH:/home/\\$USER/" + installDir + "/bin";

        String commandStr = "echo \\\"" + InstallJavaOnHostsRequest.class.getCanonicalName() + "\\\" ; " + "echo \\\"" + requestId.toString() + "\\\" ; " + "if [ -f " + java + " ] ; then MD5_JAVA=`md5sum " + java + "` ; " + "MD5_JAVA=${MD5_JAVA:0:32} ; " + " if [ \\\"$MD5_JAVA\\\" == \\\"" + md5sum + "\\\" ] ; then " + "echo \\\"state=Java installation exists. MD5 sum on file is good. Not downloading again.\\\" ; " + "else " + " echo \\\"state=Removing bad java version, downloading new version.\\\" ; " + " rm " + java + " ; wget -q http://lucan.sics.se/" + java + " ; " + " fi ; " + "else wget -q http://lucan.sics.se/" + java + " ; " + " fi ; " + " if [ -f /home/$USER/.bashrc ] ; then " + " FOUND_JH=`grep '" + javaBash + "' /home/$USER/.bashrc -c` ; " + " if [ $FOUND_JH -lt 1 ] ; then " + " JH='export JAVA_HOME=/home/$USER/" + installDir + "' ; " + " echo ${JH} >> /home/$USER/.bashrc ;" + " fi ; " + " FOUND_P=`grep '" + pathBash + "' /home/$USER/.bashrc -c` ;" + " if [ $FOUND_P -lt 1 ] ; then " + " P2='PATH=$PATH:/home/$USER/" + installDir + "/bin' ;" + " echo export ${P2} >> /home/$USER/.bashrc ;" + " fi ; fi ; " + " source /home/$USER/.bashrc ; " + " if [ ! -d /home/$USER/installDir ] ; then " + " tar zxf " + java + " ; " //					+ " chmod +x " + java + " ; "
                //					+ "echo \\\"YES\\\" > accept_license | ./" + java + " < accept_license ;"
                + " fi ; " + " IS_JAVA=`which java` ; echo \\\"java=${IS_JAVA:(-4)}\\\" ;";
        sendSshCommand(requestId, sessionId, commandStr);

    }

    private void sendSshCommand(UUID requestId, int sessionId, String commandStr) {
        SshCommandRequest command = new SshCommandRequest(requestId, sessionId,
                commandStr, PlanetLabConfiguration.DEFAULT_SSH_COMMAND_TIMEOUT,
                true);
        trigger(command, sshPort);
    }

    private void installDaemon() {
    }
    private Handler<SshCommandResponse> handleCommandResponse = new Handler<SshCommandResponse>() {

        public void handle(SshCommandResponse event) {

            logger.info("CommandResponse received : ");
            logger.info(event.getCommandResponse());

            String responseStr = event.getCommandResponse();
            Scanner scanner = new Scanner(responseStr);
            try {
                String header = scanner.nextLine();
                String uuid = scanner.nextLine();
                UUID requestId = UUID.fromString(uuid);
                ActionRequest requestEvent = connectionRequests.get(requestId);
                boolean authenticated = (event.getSessionId() == -1) ? false : true;

                if (header.compareToIgnoreCase(StartDaemonOnHostsRequest.class.getCanonicalName()) == 0) {

                    if (requestEvent instanceof StartDaemonOnHostsRequest) {
                        StartDaemonOnHostsRequest startReq = (StartDaemonOnHostsRequest) requestEvent;
                        logger.info("Sending start daemon response");
                        Set<String> hosts = startReq.getHosts();
                        trigger(new StartDaemonOnHostsResponse(startReq, hosts, authenticated), servicesPort);
                    } else {
                        logger.warn("Start Daemon response request object was of unexpected type");
                    }

                } else if (header.compareToIgnoreCase(StopDaemonOnHostsRequest.class.getCanonicalName()) == 0) {

                    ActionRequest req = connectionRequests.get(requestId);

                    if (req instanceof StopDaemonOnHostsRequest) {
                        StopDaemonOnHostsRequest stopReq = (StopDaemonOnHostsRequest) req;
                        logger.info("Sending stop daemon response");
                        Set<String> hosts = stopReq.getHosts();
                        trigger(new StopDaemonOnHostsResponse(stopReq, hosts, authenticated), servicesPort);
                    } else {
                        logger.warn("Stop Daemon response request object was of unexpected type");
                    }
                } else if (header.compareToIgnoreCase(GetStatusRequest.class.getCanonicalName()) == 0) {

                    ServicesStatus.Installation daemonInstalled = ServicesStatus.Installation.NOT_INSTALLED;
                    ServicesStatus.Installation javaInstalled = ServicesStatus.Installation.NOT_INSTALLED;
                    ServicesStatus.Program daemonRunning = ServicesStatus.Program.STOPPED;

                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        if (processStatus(line, "daemon", "true") == true) {
                            daemonInstalled = ServicesStatus.Installation.INSTALLED;
                        }
                        if (processStatus(line, "java", "java") == true) {
                            javaInstalled = ServicesStatus.Installation.INSTALLED;
                        }
                        if (processStatus(line, "daemonService", "true") == true) {
                            daemonRunning = ServicesStatus.Program.RUNNING;
                        }
                    }

                    Host host = sessionHosts.get(event.getSessionId());
                    ServicesStatus status = hostStatus.get(host);
                    status.setConnected(true);
                    status.setDaemon(daemonInstalled);
                    status.setJava(javaInstalled);
                    status.setDaemonRunning(daemonRunning);
                    hostStatus.put(host, status);
                    sendActionResponse(host, requestEvent.getAction(), status, authenticated);

                } else if (header.compareToIgnoreCase(InstallJavaOnHostsRequest.class.getCanonicalName()) == 0) {

                    Host host = sessionHosts.get(event.getSessionId());
                    ServicesStatus status = hostStatus.get(host);
                    if (status == null) {
                        status = new ServicesStatus(true);
                    } else {
                        status.setConnected(true);
                    }

                    boolean success = false;
                    while (scanner.hasNextLine() && success == false) {
                        success = processStatus(scanner.nextLine(), "java", "java");
                    }

                    if (success == true) {
                        status.setJava(ServicesStatus.Installation.INSTALLED);
                    } else {
                        status.setJava(ServicesStatus.Installation.NOT_INSTALLED);
                    }
                    hostStatus.put(host, status);
                    sendActionResponse(host, requestEvent.getAction(), status, authenticated);


                } else {
                    logger.warn("Unrecognised header from ssh command response");
                    return;
                }
            } finally {
                scanner.close();
            }

            // parse the response
            // (1) see if it's checking for java/daemon installation
            // (2) see if it has invoked daemon.jar

        }
    };

    protected boolean processStatus(String aLine, String expectedName,
            String expectedValue) {
        boolean started = false;
        Scanner scanner = new Scanner(aLine);
        scanner.useDelimiter("=");
        if (scanner.hasNext()) {
            String name = scanner.next();
            if (name == null || scanner.hasNext() == false) {
                return false;
            }
            String value = scanner.next();
            if (value == null) {
                return false;
            }
            if (name.compareToIgnoreCase(expectedName) == 0) {
                if (value.compareToIgnoreCase(expectedValue) == 0) {
                    started = true;
                }
            }
        }
        scanner.close();
        return started;
    }
    private Handler<UploadFileResponse> handleUploadFileResponse = new Handler<UploadFileResponse>() {

        public void handle(UploadFileResponse event) {

            logger.info("Uploaded file {}", event.getFile().getName());

            ActionRequest requestEvent = connectionRequests.get(event.getRequestId());
            int sessionId = event.getSessionId();
            Host host = sessionHosts.get(sessionId);
            ServicesStatus status = hostStatus.get(host);
            if (event.getFile().compareTo(new File(PlanetLabConfiguration.DEFAULT_DAEMON_JAR_FILE)) == 0) {
                status.setDaemon(ServicesStatus.Installation.INSTALLED);
            } else if (event.getFile().compareTo(new File(PlanetLabConfiguration.DEFAULT_JAVA_REDHAT_INSTALLATION_FILE)) == 0) {
                status.setJava(ServicesStatus.Installation.INSTALLING);
            }
            hostStatus.put(host, status);

            boolean sshAuthenticationSuccess = (event.getSessionId() == -1) ? false : true;

            sendActionResponse(host, requestEvent.getAction(), status, sshAuthenticationSuccess);
        }
    };

    private void sendActionResponse(Host host,
            ExperimentServicesComponent.Action action, ServicesStatus status,
            boolean sshAuthenticationSuccess) {
        trigger(new ActionProgressResponse(host, action, status, sshAuthenticationSuccess), servicesPort);
    }

    private class Installer extends Thread {

        private final ExecutorService threadPool;
        private final ConcurrentLinkedQueue<Future<InstallationResult>> tasks;
        private final File file;
        private final String targetDir;
        private Set<PLabHost> hosts;

        public Installer(int numThreads, File file, String targetDir) {
            this.threadPool = Executors.newFixedThreadPool(numThreads);
            this.tasks = new ConcurrentLinkedQueue<Future<InstallationResult>>();
            this.file = file;
            this.targetDir = targetDir;
        }

        public void setHosts(Set<PLabHost> hosts) {
            this.hosts = new HashSet<PLabHost>();
            for (PLabHost h : hosts) {
                hosts.add(new PLabHost(h));
            }
        }

        public void performLookups() {
            int i = 0;
            for (PLabHost host : hosts) {
                Future<InstallationResult> task = threadPool.submit(new InstallFile(host, file, targetDir));
                tasks.add(task);
            }
        }

        public void run() {
            Future<InstallationResult> task;
            try {
                // for all tasks
                while ((task = tasks.poll()) != null) {
                    // block until task is done
                    InstallationResult result = (InstallationResult) task.get();
                    if (result != null) {
                        logger.info("Installation for host {} was {}", result.getHostname(), result.isSuccess());
                    }
                }
                threadPool.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

        }
    }

    private class InstallationResult {

        private final boolean success;
        private final String hostname;

        public InstallationResult(boolean success, String hostname) {
            this.success = success;
            this.hostname = hostname;
        }

        public String getHostname() {
            return hostname;
        }

        public boolean isSuccess() {
            return success;
        }
    }

    private class InstallFile implements Callable<InstallationResult> {

        private final PLabHost host;
        private final File file;
        private final String targetDir;

        public InstallFile(PLabHost host, File file, String targetDir) {
            this.host = host;
            this.file = file;
            this.targetDir = targetDir;
        }

        public InstallationResult call() {

            // connect to host using ssh
            // copy file using Scp
            // return result

            return null;
        }
    }
}
