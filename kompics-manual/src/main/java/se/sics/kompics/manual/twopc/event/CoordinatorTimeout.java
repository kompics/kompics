package se.sics.kompics.manual.twopc.event;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;


public final class CoordinatorTimeout extends Timeout {

	public CoordinatorTimeout(ScheduleTimeout request) {
		super(request);
	}
}
