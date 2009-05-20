package se.sics.kompics.kdld.daemon;

import se.sics.kompics.address.Address;

public class JobLoadResponseMsg extends DaemonResponseMessage {

	private static final long serialVersionUID = 1212312401206L;

	public enum Status {
		LOADING, POM_CREATED, ASSEMBLED, FAIL, DUPLICATE
	};

	private final Status status;

	private String msg;

	private final int jobId;

	public JobLoadResponseMsg(int jobId, Status status, DaemonAddress src, Address dest) {
		super(src, dest);
		this.jobId = jobId;
		this.status = status;
	}

	public JobLoadResponseMsg(int jobId, Status status, String msg, DaemonAddress src, Address dest) {
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
