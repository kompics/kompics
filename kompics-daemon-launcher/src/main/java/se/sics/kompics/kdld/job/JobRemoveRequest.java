package se.sics.kompics.kdld.job;

import se.sics.kompics.Request;

public class JobRemoveRequest extends Request {

	private static final long serialVersionUID = 8620116574570790715L;

	private final Job job;
	
	public JobRemoveRequest(Job job) {
		super();
		this.job = job;
	}


	public Job getJob() {
		return job;
	}
}
