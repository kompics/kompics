package se.sics.kompics.timer;

import java.util.HashSet;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Priority;
import se.sics.kompics.timer.events.CancelTimeout;
import se.sics.kompics.timer.events.ScheduleTimeout;
import se.sics.kompics.timer.events.Timeout;

/**
 * The <code>TimerHandler</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
public class TimerHandler {

	private final Component component;

	private final Channel setTimerChannel;

	private long nextTimerId;

	private final HashSet<Long> outstandingTimers;

	public TimerHandler(Component component, Channel setTimerChannel) {
		super();
		this.component = component;
		this.setTimerChannel = setTimerChannel;
		this.nextTimerId = 0;
		this.outstandingTimers = new HashSet<Long>();
	}

	public long setTimer(Timeout timerExpiredEvent,
			Channel timeoutChannel, long delay) {
		ScheduleTimeout event = new ScheduleTimeout(++nextTimerId,
				timerExpiredEvent, timeoutChannel, component, delay);

		outstandingTimers.add(nextTimerId);
		component.triggerEvent(event, setTimerChannel, Priority.HIGH);
		return nextTimerId;
	}

	public void cancelTimer(long timerId) {
		CancelTimeout event = new CancelTimeout(component, timerId);
		component.triggerEvent(event, setTimerChannel, Priority.HIGH);
		outstandingTimers.remove(timerId);
	}

	public void cancelAllOutstandingTimers() {
		for (long timerId : outstandingTimers) {
			CancelTimeout event = new CancelTimeout(component, timerId);
			component.triggerEvent(event, setTimerChannel, Priority.HIGH);
		}
		outstandingTimers.clear();
	}

	public boolean isOustandingTimer(long timerId) {
		return outstandingTimers.contains(timerId);
	}

	public void handledTimerExpired(long timerId) {
		outstandingTimers.remove(timerId);
	}
}
