package se.sics.kompics.wan.master;


import java.util.ArrayList;
import java.util.List;

import se.sics.kompics.Event;

public class StopJobOnHosts extends Event {

	private final int jobId;
	private final List<Integer> listHosts;
	
	public StopJobOnHosts(int jobId) {
		this.jobId = jobId;
		this.listHosts = new ArrayList<Integer>();
	}
	
	public StopJobOnHosts(int jobId, List<Integer> listHosts) {
		this.jobId = jobId;
		this.listHosts = listHosts;
	}

	public int getJobId() {
		return jobId;
	}
	
	public List<Integer> getListHosts() {
		return listHosts;
	}
}
