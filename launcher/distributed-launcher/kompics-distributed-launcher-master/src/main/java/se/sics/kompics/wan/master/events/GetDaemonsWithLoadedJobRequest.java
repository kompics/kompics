package se.sics.kompics.wan.master.events;

import se.sics.kompics.Request;


public class GetDaemonsWithLoadedJobRequest extends Request {

	private final int jobId;
	
	public GetDaemonsWithLoadedJobRequest(int jobId) {
		this.jobId = jobId;		
	}
	
	public int getJobId() {
		return jobId;
	}
}
