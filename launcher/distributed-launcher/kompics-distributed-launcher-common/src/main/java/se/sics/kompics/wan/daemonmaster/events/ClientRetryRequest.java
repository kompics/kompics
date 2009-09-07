package se.sics.kompics.wan.daemonmaster.events;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;


public final class ClientRetryRequest extends Timeout {

	private ConnectMasterRequest request;

	private int retriesLeft;

	public ClientRetryRequest(ScheduleTimeout scheduleTimeout, int retriesLeft,
			ConnectMasterRequest request) {
		super(scheduleTimeout);
		this.retriesLeft = retriesLeft;
		this.request = request;
	}

	public ConnectMasterRequest getRequest() {
		return request;
	}

	public int getRetriesLeft() {
		return retriesLeft;
	}
}
