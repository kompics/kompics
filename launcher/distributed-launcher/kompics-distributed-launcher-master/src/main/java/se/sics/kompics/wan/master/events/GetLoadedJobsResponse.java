package se.sics.kompics.wan.master.events;

import java.util.HashSet;
import java.util.Set;

import se.sics.kompics.Response;
import se.sics.kompics.wan.job.Job;

public class GetLoadedJobsResponse extends Response {

    private final Set<Job> jobs;

    public GetLoadedJobsResponse(GetLoadedJobsRequest request, Set<Job> jobs) {
        super(request);
        this.jobs = new HashSet<Job>(jobs);
    }

    public Set<Job> getJobs() {
        return jobs;
    }
}
