package se.sics.kompics.timer.events;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

/**
 * 
 * @author Cosmin Arad
 * @version $TimerSignalEvent.java 24 2008-04-14 18:17:59Z cosmin $
 */
@EventType
public abstract class TimerSignalEvent implements Event {

	private long timerId;

	public long getTimerId() {
		return timerId;
	}

	public void setTimerId(long timerId) {
		this.timerId = timerId;
	}
}
