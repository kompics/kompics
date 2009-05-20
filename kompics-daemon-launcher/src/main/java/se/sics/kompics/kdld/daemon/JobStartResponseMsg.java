package se.sics.kompics.kdld.daemon;

import se.sics.kompics.address.Address;

public class JobStartResponseMsg extends DaemonResponseMessage {


	private static final long serialVersionUID = -8081345362548135164L;

	public enum Status {
		SUCCESS, NOT_LOADED, ERROR
	};

	private final Status status;

	private String msg;

	private final int jobId;

	public JobStartResponseMsg(int jobId, Status status, DaemonAddress src, Address dest) {
		super(src, dest);
		this.jobId = jobId;
		this.status = status;
	}

	public JobStartResponseMsg(int jobId, Status status, String msg, DaemonAddress src, Address dest) {
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
