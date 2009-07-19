package se.sics.kompics.wan.daemon.indexer;

import java.util.HashSet;
import java.util.Set;

import se.sics.kompics.Response;
import se.sics.kompics.wan.job.Job;

public class ListJobsLoadedResponse extends Response {
	
	private final Set<Job> setJobs;
	
	public ListJobsLoadedResponse(ListJobsLoadedRequest request, Set<Job> setJobs)
	{
		super(request);
		this.setJobs = new HashSet<Job>(setJobs);
	}


	public Set<Job> getSetJobs() {
		return setJobs;
	}
}
