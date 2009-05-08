package se.sics.kompics.kdld.job;

import se.sics.kompics.Request;

public class JobReadFromExecuting extends Request {

	private static final long serialVersionUID = 8620116574570790715L;

	private final int jobId;
	
	public JobReadFromExecuting(int id) {
		super();
		this.jobId = id;
	}

	public int getJobId() {
		return jobId;
	}

}
