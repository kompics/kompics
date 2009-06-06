package se.sics.kompics.wan.job;

import se.sics.kompics.Response;

public class JobReadFromExecutingResponse extends Response {

	private static final long serialVersionUID = 8620116574570790715L;

	private final int jobId;
	private final String msg;
	
	public JobReadFromExecutingResponse(JobWriteToExecutingRequest request, int id, String msg) {
		super(request);
		this.jobId = id;
		this.msg = msg;
	}
	
	public JobReadFromExecutingResponse(JobReadFromExecutingRequest request, int id, String msg) {
		super(request);
		this.jobId = id;
		this.msg = msg;
	}

	public int getJobId() {
		return jobId;
	}

	public String getMsg() {
		return msg;
	}
}
