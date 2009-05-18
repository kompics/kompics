package se.sics.kompics.kdld.master;

import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;

public final class ClientRefreshPeer extends Timeout {

	public ClientRefreshPeer(SchedulePeriodicTimeout request) {
		super(request);
	}
}