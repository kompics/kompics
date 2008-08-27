package se.sics.kompics.timer;

import java.util.HashSet;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Priority;
import se.sics.kompics.timer.events.CancelAlarm;
import se.sics.kompics.timer.events.SetAlarm;
import se.sics.kompics.timer.events.Alarm;

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

	public long setTimer(Alarm timerExpiredEvent,
			Channel timeoutChannel, long delay) {
		SetAlarm event = new SetAlarm(++nextTimerId,
				timerExpiredEvent, timeoutChannel, component, delay);

		outstandingTimers.add(nextTimerId);
		component.triggerEvent(event, setTimerChannel, Priority.HIGH);
		return nextTimerId;
	}

	public void cancelTimer(long timerId) {
		CancelAlarm event = new CancelAlarm(component, timerId);
		component.triggerEvent(event, setTimerChannel, Priority.HIGH);
		outstandingTimers.remove(timerId);
	}

	public void cancelAllOutstandingTimers() {
		for (long timerId : outstandingTimers) {
			CancelAlarm event = new CancelAlarm(component, timerId);
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
