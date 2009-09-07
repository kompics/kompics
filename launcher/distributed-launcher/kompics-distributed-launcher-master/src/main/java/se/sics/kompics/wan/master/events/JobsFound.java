package se.sics.kompics.wan.master.events;

import java.util.HashSet;
import java.util.Set;

import se.sics.kompics.Event;
import se.sics.kompics.address.Address;
import se.sics.kompics.wan.job.Job;

public class JobsFound extends Event {

    private final Address addr;
    private final Set<Job> jobs;

    public JobsFound(Address addr, Job job) {
        this.addr = addr;
        this.jobs = new HashSet<Job>();
        this.jobs.add(job);
    }

    public JobsFound(Address addr, Set<Job> jobs) {
        this.addr = addr;
        this.jobs = new HashSet<Job>(jobs);
    }

    public Address getAddr() {
        return addr;
    }

    public Set<Job> getJobs() {
        return jobs;
    }
}
