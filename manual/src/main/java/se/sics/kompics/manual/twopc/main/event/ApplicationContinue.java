package se.sics.kompics.manual.twopc.main.event;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public final class ApplicationContinue extends Timeout {

	public ApplicationContinue(ScheduleTimeout request) {
		super(request);
	}
}
