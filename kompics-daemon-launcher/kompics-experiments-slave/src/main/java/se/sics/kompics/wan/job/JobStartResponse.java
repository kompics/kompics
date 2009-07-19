package se.sics.kompics.wan.job;

import java.io.Serializable;

import se.sics.kompics.Response;


public class JobStartResponse extends Response implements Serializable {

	private static final long serialVersionUID = 2993973136500802022L;

	public enum Status {
		SUCCESS, FAIL, DUPLICATE, NOT_LOADED
	};

	private final Status status;

	private final int jobId;
	
	public JobStartResponse(JobStartRequest request, int jobId, Status status) {
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
