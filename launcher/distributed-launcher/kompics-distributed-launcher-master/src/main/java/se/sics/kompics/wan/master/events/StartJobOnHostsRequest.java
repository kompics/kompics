package se.sics.kompics.wan.master.events;


import java.util.ArrayList;
import java.util.List;

import se.sics.kompics.Request;

public class StartJobOnHostsRequest extends Request {

	private final int jobId;
	private final int numPeersPerHost;
	private final List<Integer> listHosts;
	
	public StartJobOnHostsRequest(int jobId, int numPeersPerHost) {
		this.jobId = jobId;
		this.numPeersPerHost = numPeersPerHost;
		this.listHosts = new ArrayList<Integer>();
	}
	
	public StartJobOnHostsRequest(int jobId, List<Integer> listHosts, int numPeersPerHost) {
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
