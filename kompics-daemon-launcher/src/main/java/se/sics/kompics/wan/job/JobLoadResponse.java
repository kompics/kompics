package se.sics.kompics.wan.job;

import se.sics.kompics.Response;



public class JobLoadResponse extends Response {

	private static final long serialVersionUID = 1212312401206L;

	public enum Status {
		POM_CREATED, ASSEMBLED, FAIL, DUPLICATE
	};

	private final Status status;

	private final int jobId;

	public JobLoadResponse(JobLoadRequest request, int jobId, Status status) {
		super(request);
		this.jobId = jobId;
		this.status = status;
	}

	public int getJobId() {
		return jobId;
	}

	public Status getStatus() {
		return status;
	}

}
