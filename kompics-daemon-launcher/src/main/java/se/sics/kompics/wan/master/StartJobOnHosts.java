package se.sics.kompics.wan.master;


import java.util.ArrayList;
import java.util.List;

import se.sics.kompics.Event;

public class StartJobOnHosts extends Event {

	private final int jobId;
	private final int numPeersPerHost;
	private final List<Integer> listHosts;
	
	public StartJobOnHosts(int jobId, int numPeersPerHost) {
		this.jobId = jobId;
		this.numPeersPerHost = numPeersPerHost;
		this.listHosts = new ArrayList<Integer>();
	}
	
	public StartJobOnHosts(int jobId, List<Integer> listHosts, int numPeersPerHost) {
		this.jobId = jobId;
		this.numPeersPerHost = numPeersPerHost;
		this.listHosts = listHosts;
	}

	public int getJobId() {
		return jobId;
	}
	
	public int getNumPeersPerHost() {
		return numPeersPerHost;
	}
	
	public List<Integer> getListHosts() {
		return listHosts;
	}
}
