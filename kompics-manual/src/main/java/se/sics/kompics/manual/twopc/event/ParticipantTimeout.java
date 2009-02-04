package se.sics.kompics.manual.twopc.event;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public final class ParticipantTimeout extends Timeout {

	public ParticipantTimeout(ScheduleTimeout request) {
		super(request);
	}
}
