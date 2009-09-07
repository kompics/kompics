package se.sics.kompics.wan.master.events;

import se.sics.kompics.Request;


public class GetLoadedJobsForDaemonRequest extends Request {

	private final int daemonId;

	public GetLoadedJobsForDaemonRequest(int daemonId) {
		this.daemonId = daemonId;
	}

	public int getDaemonId() {
		return daemonId;
	}
}
