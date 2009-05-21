package se.sics.kompics.kdld.daemon.indexer;

import java.util.HashSet;
import java.util.Set;

import se.sics.kompics.Event;
import se.sics.kompics.kdld.job.Job;

public class JobsFound extends Event {
	
	private final Set<Job> setJobs;
	
	public JobsFound(Set<Job> setJobs)
	{
		this.setJobs = new HashSet<Job>(setJobs);
	}


	public Set<Job> getSetJobs() {
		return setJobs;
	}
}
