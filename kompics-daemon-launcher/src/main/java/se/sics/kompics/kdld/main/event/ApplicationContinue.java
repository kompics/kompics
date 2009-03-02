package se.sics.kompics.kdld.main.event;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;


public final class ApplicationContinue extends Timeout {

	public ApplicationContinue(ScheduleTimeout request) {
		super(request);
	}
}
