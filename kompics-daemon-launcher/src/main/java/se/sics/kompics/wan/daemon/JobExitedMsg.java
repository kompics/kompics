package se.sics.kompics.wan.daemon;

import se.sics.kompics.address.Address;
import se.sics.kompics.wan.job.JobExited;

public class JobExitedMsg extends DaemonResponseMessage {


	private static final long serialVersionUID = -5012778931157572267L;

	private final JobExited.Status status;

	private String msg;

	private final int jobId;

	public JobExitedMsg(JobExited job, DaemonAddress src, Address dest) {
		super(src, dest);
		this.jobId = job.getJobId();
		this.status = job.getStatus();
		this.msg = job.getMsg();
	}


	public int getJobId() {
		return jobId;
	}

	public JobExited.Status getStatus() {
		return status;
	}

	public String getMsg() {
		return msg;
	}

}
