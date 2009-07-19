package se.sics.kompics.wan.daemon;

import se.sics.kompics.address.Address;
import se.sics.kompics.wan.job.JobReadFromExecutingResponse;
import se.sics.kompics.wan.masterdaemon.DaemonAddress;

public class JobReadFromExecutingResponseMsg extends DaemonResponseMessage {


	private static final long serialVersionUID = 3626423419664569L;

	private String msg;

	private final int jobId;

	public JobReadFromExecutingResponseMsg(int jobId, DaemonAddress src, Address dest) {
		super(src, dest);
		this.jobId = jobId;
	}

	public JobReadFromExecutingResponseMsg(int jobId, String msg, DaemonAddress src, Address dest) {
		this(jobId, src, dest);
		this.msg = msg;
	}
	
	public JobReadFromExecutingResponseMsg(JobReadFromExecutingResponse job, DaemonAddress src, Address dest) {
		this(job.getJobId(), src, dest);
		this.msg = job.getMsg();
	}

	public int getJobId() {
		return jobId;
	}

	public String getMsg() {
		return msg;
	}

}
