package se.sics.kompics.wan.master;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;


public final class UserInputTimeout extends Timeout {

	public UserInputTimeout(ScheduleTimeout request) {
		super(request);
	}

}