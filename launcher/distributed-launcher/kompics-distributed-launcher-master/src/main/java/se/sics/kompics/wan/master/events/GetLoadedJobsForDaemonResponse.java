package se.sics.kompics.wan.master.events;

import java.util.HashSet;
import java.util.Set;

import se.sics.kompics.Response;
import se.sics.kompics.wan.job.Job;
import se.sics.kompics.wan.masterdaemon.events.DaemonAddress;

public class GetLoadedJobsForDaemonResponse extends Response {

    private final DaemonAddress daemon;
    private final Set<Job> setJobs;


    public GetLoadedJobsForDaemonResponse(GetLoadedJobsForDaemonRequest request, DaemonAddress daemon, Set<Job> jobs) {
        super(request);
        this.daemon = daemon;
        setJobs = new HashSet<Job>(jobs);
    }

    public DaemonAddress getDaemon() {
        return daemon;
    }

    
    public Set<Job> getSetJobs() {
        return setJobs;
    }
}
