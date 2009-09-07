package se.sics.kompics.wan.masterdaemon.events;

import se.sics.kompics.address.Address;
import se.sics.kompics.wan.job.JobStopResponse;

public class JobStopResponseMsg extends DaemonResponseMsg {


	private static final long serialVersionUID = -660700758791394058L;

	private final JobStopResponse.Status status;

	private String msg;

	private final int jobId;

	public JobStopResponseMsg(int jobId, JobStopResponse.Status status, DaemonAddress src, Address dest) {
		super(src, dest);
		this.jobId = jobId;
		this.status = status;
	}

	public JobStopResponseMsg(int jobId, JobStopResponse.Status status, String msg, DaemonAddress src, Address dest) {
		this(jobId, status, src, dest);
		this.msg = msg;
	}

	public JobStopResponseMsg(JobStopResponse response, DaemonAddress src, Address dest) {
		super(src, dest);
		this.jobId = response.getJobId();
		this.status = response.getStatus();
		this.msg = response.getMsg();
	}

	
	public int getJobId() {
		return jobId;
	}

	public JobStopResponse.Status getStatus() {
		return status;
	}

	public String getMsg() {
		return msg;
	}

}
