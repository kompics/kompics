package tbn.timer.events;

import tbn.api.Event;

public class TimerExpiredEvent implements Event {

	private long timerId;
	
	public long getTimerId() {
		return timerId;
	}

	public void setTimerId(long timerId) {
		this.timerId = timerId;
	}
}
