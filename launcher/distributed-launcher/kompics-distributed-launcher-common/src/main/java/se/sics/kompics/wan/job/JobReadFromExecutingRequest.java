package se.sics.kompics.wan.job;

import se.sics.kompics.Request;
import se.sics.kompics.wan.masterdaemon.events.JobReadFromExecutingRequestMsg;

public class JobReadFromExecutingRequest extends Request {

	private static final long serialVersionUID = 8620116574570790715L;

	private final int jobId;
	
	public JobReadFromExecutingRequest(JobReadFromExecutingRequestMsg msg) {
		super();
		this.jobId = msg.getJobId();
	}
	
	public JobReadFromExecutingRequest(int id) {
		super();
		this.jobId = id;
	}

	public int getJobId() {
		return jobId;
	}

}
