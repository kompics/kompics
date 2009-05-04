package se.sics.kompics.kdld.daemon;

import se.sics.kompics.address.Address;

public class JobStopResponse extends DaemonResponseMessage {


	private static final long serialVersionUID = -660700758791394058L;

	public enum Status {
		ALREADY_STOPPED, STOPPED, FAILED_TO_STOP
	};

	private final Status status;

	private String msg;

	private final int jobId;

	public JobStopResponse(int jobId, Status status, DaemonAddress src, Address dest) {
		super(src, dest);
		this.jobId = jobId;
		this.status = status;
	}

	public JobStopResponse(int jobId, Status status, String msg, DaemonAddress src, Address dest) {
		this(jobId, status, src, dest);
		this.msg = msg;
	}

	public int getJobId() {
		return jobId;
	}

	public Status getStatus() {
		return status;
	}

	public String getMsg() {
		return msg;
	}

}
