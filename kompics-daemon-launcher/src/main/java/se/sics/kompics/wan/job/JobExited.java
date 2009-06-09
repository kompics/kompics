package se.sics.kompics.wan.job;

import se.sics.kompics.Event;



public class JobExited extends Event {

	private final int exitValue;

	private String msg;

	private final int jobId;

	public JobExited(int jobId, int exitValue, String exitMsg) {
		super();
		this.jobId = jobId;
		this.exitValue = exitValue;
		this.msg = exitMsg;
	}

	public int getJobId() {
		return jobId;
	}

	public int getExitValue() {
		return exitValue;
	}

	public String getMsg() {
		return msg;
	}

}
