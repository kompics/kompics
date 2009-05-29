package se.sics.kompics.wan.master;

import se.sics.kompics.Event;

public class StartJobOnHosts extends Event {

	private final int jobId;
	private final int numPeersPerHost;

	public StartJobOnHosts(int jobId, int numPeersPerHost) {
		this.jobId = jobId;
		this.numPeersPerHost = numPeersPerHost;
	}

	public int getJobId() {
		return jobId;
	}
	
	public int getNumPeersPerHost() {
		return numPeersPerHost;
	}
}
