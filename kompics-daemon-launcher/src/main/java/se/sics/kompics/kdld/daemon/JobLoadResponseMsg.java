package se.sics.kompics.kdld.daemon;

import se.sics.kompics.address.Address;
import se.sics.kompics.kdld.job.JobLoadResponse;

public class JobLoadResponseMsg extends DaemonResponseMessage {

	private static final long serialVersionUID = 1212312401206L;

	private final JobLoadResponse.Status status;

	private final int jobId;

	public JobLoadResponseMsg(JobLoadResponse event, DaemonAddress src, Address dest) {
		super(src, dest);
		this.jobId = event.getJobId();
		this.status = event.getStatus();
	}
	
	public JobLoadResponseMsg(int jobId, JobLoadResponse.Status status, DaemonAddress src, Address dest) {
		super(src, dest);
		this.jobId = jobId;
		this.status = status;
	}

	public int getJobId() {
		return jobId;
	}

	public JobLoadResponse.Status getStatus() {
		return status;
	}

}
