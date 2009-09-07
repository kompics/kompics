package se.sics.kompics.wan.job;

import java.util.List;
import se.sics.kompics.wan.masterdaemon.events.JobExecRequestMsg;

public class JobExecRequest extends Job {

    private static final long serialVersionUID = 987654335182L;

    private final String hostname;

    private final int numPeers;

    private final int port;
    private final int webPort;

    private final int bootPort;
    private final int bootWebPort;
    private final int monitorPort;
    private final int monitorWebPort;

    private final String bootHost;
    private final String monitorHost;

    public JobExecRequest(String hostname, int numPeers, 
            String groupId, String artifactId, String version,
            String repoId, String repoUrl,
            String mainClass, List<String> args,
            int port, int webPort,
            String bootHost, int bootPort, int bootWebPort,
            String monitorHost, int monitorPort, int monitorWebPort) {
        super(groupId, artifactId, version, mainClass, args, repoId, repoUrl);
        this.hostname = hostname;
        this.numPeers = numPeers;
        this.port = port;
        this.webPort = webPort;
        this.bootPort = bootPort;
        this.bootWebPort = bootWebPort;
        this.monitorPort = monitorPort;
        this.monitorWebPort = monitorWebPort;
        this.bootHost = bootHost;
        this.monitorHost = monitorHost;
    }

    public JobExecRequest(Job job, JobExecRequestMsg msg) {
        this(msg.getHostname(), msg.getNumPeers(),
                job.getGroupId(), job.getArtifactId(), job.getVersion(),
                job.getRepoId(), job.getRepoUrl(),
                job.getMainClass(), job.getArgs(), 
                msg.getPort(), msg.getWebPort(),
                msg.getBootHost(), msg.getBootPort(), msg.getBootWebPort(),
                msg.getMonitorHost(), msg.getMonitorPort(), msg.getMonitorWebPort());
    }

    public int getNumPeers() {
        return numPeers;
    }

    public int getBootPort() {
        return bootPort;
    }

    public int getBootWebPort() {
        return bootWebPort;
    }

    public int getMonitorPort() {
        return monitorPort;
    }

    public int getMonitorWebPort() {
        return monitorWebPort;
    }

    public int getPort() {
        return port;
    }

    public int getWebPort() {
        return webPort;
    }

    public String getHostname() {
        return hostname;
    }

    public String getMonitorHost() {
        return monitorHost;
    }

    public String getBootHost() {
        return bootHost;
    }

    
}
