package se.sics.kompics.wan.daemon;

import se.sics.kompics.address.Address;

public class JobMessageResponse extends DaemonResponseMessage {


	private static final long serialVersionUID = -3859172569527999947L;

	public enum Status {
		STOPPED, SUCCESS, FAIL
	};

	private final Status status;

	private String msg;

	private final int jobId;

	public JobMessageResponse(int jobId, Status status, DaemonAddress src, Address dest) {
		super(src, dest);
		this.jobId = jobId;
		this.status = status;
	}

	public JobMessageResponse(int jobId, Status status, String msg, DaemonAddress src, Address dest) {
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
