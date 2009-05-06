package se.sics.kompics.kdld.job;

import se.sics.kompics.Event;

public class JobExited extends Event {


	public enum Status {
		EXITED_NORMALLY, EXITED_WITH_ERROR
	};

	private final Status status;

	private String msg;

	private final int jobId;

	public JobExited(int jobId, Status status) {
		super();
		this.jobId = jobId;
		this.status = status;
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
