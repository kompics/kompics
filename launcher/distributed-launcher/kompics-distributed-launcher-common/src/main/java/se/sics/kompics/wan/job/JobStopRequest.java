package se.sics.kompics.wan.job;

import se.sics.kompics.Request;

/**
 * 
 *  Can include either the jobId or
 *  the jobId and the job description as a maven artifact.
 *  
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class JobStopRequest extends Request {

    private final int jobId;
    private final String hostname;

    public JobStopRequest(int jobId, String hostname) {
        this.jobId = jobId;
        this.hostname = hostname;

    }

    public JobStopRequest(int jobId) {
        this.jobId = jobId;
        this.hostname = "";

    }

    public int getJobId() {
        return jobId;
    }

    public String getHostname() {
        return hostname;
    }
}
