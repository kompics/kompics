package se.sics.kompics.kdld.daemon;

import se.sics.kompics.address.Address;
import se.sics.kompics.kdld.job.JobStartResponse;

public class JobStartResponseMsg extends DaemonResponseMessage {


	private static final long serialVersionUID = -8081345362548135164L;

	private final JobStartResponse.Status status;

	private final int jobId;

	public JobStartResponseMsg(JobStartResponse event, DaemonAddress src, Address dest) {
		super(src, dest);
		this.jobId = event.getJobId();
		this.status = event.getStatus();
	}
	
	public JobStartResponseMsg(int jobId, JobStartResponse.Status status, DaemonAddress src, Address dest) {
		super(src, dest);
		this.jobId = jobId;
		this.status = status;
	}

	public int getJobId() {
		return jobId;
	}

	public JobStartResponse.Status getStatus() {
		return status;
	}

}
