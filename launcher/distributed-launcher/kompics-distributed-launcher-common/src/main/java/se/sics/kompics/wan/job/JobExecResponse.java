package se.sics.kompics.wan.job;

import java.io.Serializable;

import se.sics.kompics.Response;


public class JobExecResponse extends Response implements Serializable {

	private static final long serialVersionUID = 299332150011022L;

	public enum Status {
		SUCCESS, FAIL, DUPLICATE, NOT_LOADED, LOADING, EXECUTING
	};

	private final Status status;

	private final int jobId;
	
	public JobExecResponse(JobExecRequest request, int jobId, Status status) {
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
