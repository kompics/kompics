package se.sics.kompics.kdld.master;

import se.sics.kompics.Request;

public final class ConnectMasterReq extends Request {

	private final int jobId;

	private final int daemonId;

	public ConnectMasterReq(int jobId, int daemonId) {
		this.jobId = jobId;
		this.daemonId = daemonId;
	}

	public int getDaemonId() {
		return daemonId;
	}

	public int getJobId() {
		return jobId;
	}
}
