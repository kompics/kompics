package se.sics.kompics.wan.masterdaemon.events;

import se.sics.kompics.address.Address;
import se.sics.kompics.wan.job.JobExecResponse;

public class JobExecResponseMsg extends DaemonResponseMsg {


	private static final long serialVersionUID = -80813451548135164L;

	private final JobExecResponse.Status status;

	private final int jobId;

	public JobExecResponseMsg(JobExecResponse event, DaemonAddress src, Address dest) {
		super(src, dest);
		this.jobId = event.getJobId();
		this.status = event.getStatus();
	}
	
	public JobExecResponseMsg(int jobId, JobExecResponse.Status status, DaemonAddress src, Address dest) {
		super(src, dest);
		this.jobId = jobId;
		this.status = status;
	}

	public int getJobId() {
		return jobId;
	}

	public JobExecResponse.Status getStatus() {
		return status;
	}

}
