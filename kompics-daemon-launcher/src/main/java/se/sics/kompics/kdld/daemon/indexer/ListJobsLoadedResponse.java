package se.sics.kompics.kdld.daemon.indexer;

import java.util.HashSet;
import java.util.Set;

import se.sics.kompics.Response;
import se.sics.kompics.kdld.job.Job;

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
