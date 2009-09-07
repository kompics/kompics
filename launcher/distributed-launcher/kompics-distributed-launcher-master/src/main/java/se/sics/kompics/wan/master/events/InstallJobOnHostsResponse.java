package se.sics.kompics.wan.master.events;

import java.net.InetAddress;
import se.sics.kompics.Response;
import se.sics.kompics.wan.job.JobLoadResponse;
import se.sics.kompics.wan.job.JobLoadResponse.Status;

public class InstallJobOnHostsResponse extends Response {

    final private InetAddress ip;
    final private int jobId;
    final private JobLoadResponse.Status status;
    final private String msg;

    public InstallJobOnHostsResponse(InstallJobOnHostsRequest request,
            InetAddress ip,
            int jobId, JobLoadResponse.Status status, String msg) {
        super(request);
        this.ip = ip;
        this.jobId = jobId;
        this.status = status;
        this.msg = msg;
    }

    public InetAddress getIp() {
        return ip;
    }
    
    public int getJobId() {
        return jobId;
    }

    public String getMsg() {
        return msg;
    }

    public Status getStatus() {
        return status;
    }


    
};
