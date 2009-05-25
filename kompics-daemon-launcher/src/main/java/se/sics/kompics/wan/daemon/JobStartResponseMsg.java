package se.sics.kompics.wan.daemon;

import se.sics.kompics.address.Address;
import se.sics.kompics.wan.job.JobStartResponse;

public class JobStartResponseMsg extends DaemonResponseMessage {


	private static final long serialVersionUID = -8081345362548135164L;

	private final JobStartResponse.Status status;

	private final int jobId;
	private final int slaveId;

	public JobStartResponseMsg(JobStartResponse event, DaemonAddress src, Address dest) {
		super(src, dest);
		this.jobId = event.getJobId();
		this.slaveId = event.getSlaveId();
		this.status = event.getStatus();
	}
	
	public JobStartResponseMsg(int jobId, int slaveId, JobStartResponse.Status status, DaemonAddress src, Address dest) {
		super(src, dest);
		this.jobId = jobId;
		this.slaveId = slaveId;
		this.status = status;
	}

	public int getJobId() {
		return jobId;
	}

	public JobStartResponse.Status getStatus() {
		return status;
	}

	public int getSlaveId() {
		return slaveId;
	}
}
