package se.sics.kompics.wan.master.events;


import java.util.ArrayList;
import java.util.List;

import se.sics.kompics.Event;

public class StopJobOnHostsRequest extends Event {

	private final int jobId;
	private final List<Integer> listHosts;
	
	public StopJobOnHostsRequest(int jobId) {
		this.jobId = jobId;
		this.listHosts = new ArrayList<Integer>();
	}
	
	public StopJobOnHostsRequest(int jobId, List<Integer> listHosts) {
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
