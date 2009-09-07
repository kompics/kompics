package se.sics.kompics.wan.job;

import se.sics.kompics.Response;
import se.sics.kompics.wan.masterdaemon.events.JobRemoveResponseMsg;

public class JobRemoveResponse extends Response {

	private static final long serialVersionUID = 8620116574570790715L;

	private final int jobId;
	private final String msg;
	private final JobRemoveResponseMsg.Status status;
	
	public JobRemoveResponse(JobRemoveRequest request, JobRemoveResponseMsg.Status status,
			int id, String msg) {
		super(request);
		this.jobId = id;
		this.status = status;
		this.msg = msg;
	}

	public int getJobId() {
		return jobId;
	}

	public JobRemoveResponseMsg.Status getStatus() {
		return status;
	}
	
	public String getMsg() {
		return msg;
	}
}
