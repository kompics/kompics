package se.sics.kompics.kdld.master;

import se.sics.kompics.Request;

public class PrintDaemonsWithLoadedJob extends Request {

	private final int jobId;
	
	public PrintDaemonsWithLoadedJob(int jobId) {
		this.jobId = jobId;		
	}
	
	public int getJobId() {
		return jobId;
	}
}
