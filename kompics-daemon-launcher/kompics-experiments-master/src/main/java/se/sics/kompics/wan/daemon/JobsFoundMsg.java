package se.sics.kompics.wan.daemon;

import java.util.HashSet;
import java.util.Set;

import se.sics.kompics.address.Address;
import se.sics.kompics.wan.job.Job;

public class JobsFoundMsg extends DaemonResponseMessage {


	private static final long serialVersionUID = -4939393819212736632L;
	private final Set<Job> setJobs;

	public JobsFoundMsg(Job job, DaemonAddress src, Address dest) {
		super(src, dest);
		setJobs = new HashSet<Job>();
		setJobs.add(job);
	}
	
	public JobsFoundMsg(Set<Job> jobs, DaemonAddress src, Address dest) {
		super(src, dest);
		setJobs = new HashSet<Job>(jobs);
	}


	public Set<Job> getSetJobs() {
		return setJobs;
	}
	
	public void addJob(Job job)
	{
		setJobs.add(job);
	}
}
