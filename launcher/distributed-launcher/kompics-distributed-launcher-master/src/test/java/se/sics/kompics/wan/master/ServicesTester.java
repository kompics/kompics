package se.sics.kompics.wan.master;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import org.apache.commons.configuration.ConfigurationException;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.services.ExperimentServicesComponent;
import se.sics.kompics.wan.services.ServicesPort;
import se.sics.kompics.wan.services.ExperimentServicesComponent.Action;
import se.sics.kompics.wan.services.ExperimentServicesComponent.ServicesStatus;
import se.sics.kompics.wan.services.events.ActionProgressResponse;
import se.sics.kompics.wan.services.events.ExperimentServicesInit;
import se.sics.kompics.wan.services.events.GetStatusRequest;
import se.sics.kompics.wan.services.events.InstallDaemonOnHostsRequest;
import se.sics.kompics.wan.services.events.InstallJavaOnHostsRequest;
import se.sics.kompics.wan.services.events.StartDaemonOnHostsRequest;
import se.sics.kompics.wan.ssh.Credentials;
import se.sics.kompics.wan.ssh.Host;
import se.sics.kompics.wan.ssh.SshComponent;
import se.sics.kompics.wan.ssh.SshCredentials;
import se.sics.kompics.wan.ssh.SshPort;
import se.sics.kompics.wan.ssh.scp.DownloadUploadMgr;
import se.sics.kompics.wan.ssh.scp.DownloadUploadPort;
import se.sics.kompics.wan.ssh.scp.ScpComponent;
import se.sics.kompics.wan.ssh.scp.ScpPort;

public class ServicesTester {

    public static final int SSH_CONNECT_TIMEOUT = 300 * 1000;
    public static final int SSH_KEY_EXCHANGE_TIMEOUT = 15000;
    public static final boolean UPLOAD = true;
    public static final String FILE_TO_COPY = "/home/jdowling/lqist.blah";
    public static final String FILE_TARGET_DIR = "/home/jdowling/";
    private static Semaphore semaphore = new Semaphore(0);
    private static final int EVENT_COUNT = 1;
    private final Logger logger = LoggerFactory.getLogger(ServicesTester.class);

    public static void setTestObj(ServicesTester testObj) {
        TestServicesComponent.testObj = testObj;
    }

    public static class SshConnectTimeout extends Timeout {

        public SshConnectTimeout(ScheduleTimeout request) {
            super(request);
        }
    }

    public static class TestServicesComponent extends ComponentDefinition {

        private Component sshComponent;
        private Component scpComponent;
        private Component downloadUploadMgComponent;
        private Component servicesComponent;
        private Component timer;
        private static ServicesTester testObj = null;
        private final HashSet<UUID> outstandingTimeouts = new HashSet<UUID>();
        private Credentials cred;
        private Set<String> hosts;
        private int numResponses = 0;

        public TestServicesComponent() {

            timer = create(JavaTimer.class);
            sshComponent = create(SshComponent.class);
            scpComponent = create(ScpComponent.class);
            downloadUploadMgComponent = create(DownloadUploadMgr.class);
            servicesComponent = create(ExperimentServicesComponent.class);

            connect(sshComponent.getNegative(DownloadUploadPort.class), downloadUploadMgComponent.getPositive(DownloadUploadPort.class));

            connect(downloadUploadMgComponent.getNegative(ScpPort.class), scpComponent.getPositive(ScpPort.class));

            connect(servicesComponent.getNegative(SshPort.class),
                    sshComponent.getPositive(SshPort.class));

            connect(servicesComponent.getNegative(Timer.class), timer.getPositive(Timer.class));

            subscribe(handleActionProgressResponse,
                    servicesComponent.getPositive(ServicesPort.class));

            subscribe(handleStart, control);

            cred = new SshCredentials("csl", "",
                    "/home/jdowling/.ssh/id_rsa", "");

            trigger(new ExperimentServicesInit(), servicesComponent.getControl());
        }
        public Handler<Start> handleStart = new Handler<Start>() {

            public void handle(Start event) {

                hosts = new HashSet<String>();
                hosts.add("evgsics1.sics.se");
//				hosts.add("evgsicsmgt.sics.se");

                UUID requestId = UUID.randomUUID();
                outstandingTimeouts.add(requestId);

                trigger(new GetStatusRequest(cred, hosts), servicesComponent.getPositive(ServicesPort.class));

            }
        };

        public Handler<ActionProgressResponse> handleActionProgressResponse = new Handler<ActionProgressResponse>() {

            public void handle(ActionProgressResponse event) {
                numResponses++;
                Action action = event.getAction();
                Host h = event.getHost();
                ServicesStatus status = event.getStatus();

                System.out.println(h.getHostname() + "; Action = " + action + " ; Daemon = " + status.getDaemon() + " ; Java = " + status.getJava() + " ; Daemon Running = " + status.getDaemonRunning());

                if (numResponses == hosts.size() && action == Action.GET_STATUS) {
                    trigger(new InstallDaemonOnHostsRequest(cred, hosts,true), servicesComponent.getPositive(ServicesPort.class));
//					trigger(new InstallJavaOnHostsRequest(cred, hosts,true), servicesComponent
//							.getPositive(ServicesPort.class));
//					trigger(new StartDaemonOnHostsRequest(cred, hosts), servicesComponent
//							.getPositive(ServicesPort.class));

                } else if (action == Action.INSTALL_DAEMON) {
                    trigger(new InstallJavaOnHostsRequest(cred, hosts, true), servicesComponent.getPositive(ServicesPort.class));
                } else if (action == Action.INSTALL_JAVA) {
                    trigger(new StartDaemonOnHostsRequest(cred, hosts), servicesComponent.getPositive(ServicesPort.class));
                } else if (action == Action.START_DAEMON) {
                    testObj.pass();
                } else {
                    testObj.fail(true);
                }
            }
        };
    };

    public ServicesTester() {
    }

//    @org.junit.Test
    @Ignore
    public void testSsh() {

        setTestObj(this);
        try {
            Configuration.init(new String[]{}, PlanetLabConfiguration.class);
            Kompics.createAndStart(ServicesTester.TestServicesComponent.class, 1);
        } catch (ConfigurationException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {
            ServicesTester.semaphore.acquire(EVENT_COUNT);
            System.out.println("Exiting unit test....");
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.err.println(e.getMessage());
        }

    }

    public void pass() {
        System.out.println("Passed unit test");
        semaphore.release();
        org.junit.Assert.assertTrue(true);
    }

    public void fail(boolean release) {
        System.out.println("Failed unit test");
        if (release == true) {
            semaphore.release();
        }
        org.junit.Assert.assertTrue(false);
    }
}
