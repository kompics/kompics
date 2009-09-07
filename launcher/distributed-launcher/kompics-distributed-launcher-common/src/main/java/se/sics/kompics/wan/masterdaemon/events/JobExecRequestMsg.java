package se.sics.kompics.wan.masterdaemon.events;

import java.util.List;
import se.sics.kompics.address.Address;

/**
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class JobExecRequestMsg extends DaemonRequestMsg {

    private static final long serialVersionUID = 14441156452L;
    private final int jobId;
    private final String mainClass;
    private final List<String> args;

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


    public JobExecRequestMsg(int jobId,
            String hostname, int numPeers,
                int port,int webPort,
            String bootHost, int bootPort, int bootWebPort,
            String monitorHost, int monitorPort, int monitorWebPort,
            String mainClass,
            List<String> args,
            Address src, DaemonAddress dest) {
        super(src, dest);
        this.jobId = jobId;
        this.hostname = hostname;
        this.numPeers = numPeers;
        this.mainClass = mainClass;
        this.args = args;
        this.port = port;
        this.webPort = webPort;
        this.bootPort = bootPort;
        this.bootWebPort = bootWebPort;
        this.monitorPort = monitorPort;
        this.monitorWebPort = monitorWebPort;
        this.bootHost = bootHost;
        this.monitorHost = monitorHost;
    }

    public int getJobId() {
        return jobId;
    }

    public List<String> getArgs() {
        return args;
    }

    public String getMainClass() {
        return mainClass;
    }


    public String getHostname() {
        return hostname;
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

    public String getMonitorHost() {
        return monitorHost;
    }

    public String getBootHost() {
        return bootHost;
    }
    
}
