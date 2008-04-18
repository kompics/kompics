package se.sics.kompics.timer;

import java.util.HashSet;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Priority;
import se.sics.kompics.timer.events.CancelPeriodicTimerEvent;
import se.sics.kompics.timer.events.CancelTimerEvent;
import se.sics.kompics.timer.events.SetPeriodicTimerEvent;
import se.sics.kompics.timer.events.SetTimerEvent;
import se.sics.kompics.timer.events.TimerSignalEvent;

/**
 * The <code>TimerHandler</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
public class TimerHandler {

	private final Component component;

	private long nextTimerId;

	private long nextPeriodicTimerId;

	private final HashSet<Long> outstandingTimers;

	private final HashSet<Long> outstandingPeriodicTimers;

	public TimerHandler(Component component) {
		super();
		this.component = component;
		this.nextTimerId = 0;
		this.outstandingTimers = new HashSet<Long>();
		this.outstandingPeriodicTimers = new HashSet<Long>();
	}

	public long setTimer(TimerSignalEvent timerExpiredEvent,
			Channel timeoutChannel, long delay) {

		if (!timeoutChannel.hasEventType(timerExpiredEvent.getClass())) {
			throw new RuntimeException("Cannot accept a "
					+ timerExpiredEvent.getClass().getCanonicalName()
					+ " on the given channel");
		}

		SetTimerEvent event = new SetTimerEvent(++nextTimerId,
				timerExpiredEvent, timeoutChannel, component, delay);

		outstandingTimers.add(nextTimerId);
		component.triggerEvent(event, Priority.HIGH);
		return nextTimerId;
	}

	public long setPeriodicTimer(TimerSignalEvent timerExpiredEvent,
			Channel timeoutChannel, long delay, long period) {

		if (!timeoutChannel.hasEventType(timerExpiredEvent.getClass())) {
			throw new RuntimeException("Cannot accept a "
					+ timerExpiredEvent.getClass().getCanonicalName()
					+ " on the given channel");
		}

		SetPeriodicTimerEvent event = new SetPeriodicTimerEvent(
				++nextPeriodicTimerId, timerExpiredEvent, timeoutChannel,
				component, delay, period);

		outstandingPeriodicTimers.add(nextTimerId);
		component.triggerEvent(event, Priority.HIGH);
		return nextTimerId;
	}

	public void cancelTimer(long timerId) {
		CancelTimerEvent event = new CancelTimerEvent(component, timerId);
		component.triggerEvent(event, Priority.HIGH);
		outstandingTimers.remove(timerId);
	}

	public void cancelPeriodicTimer(long timerId) {
		CancelPeriodicTimerEvent event = new CancelPeriodicTimerEvent(
				component, timerId);
		component.triggerEvent(event, Priority.HIGH);
		outstandingPeriodicTimers.remove(timerId);
	}

	public boolean isOustandingTimer(long timerId) {
		return outstandingTimers.contains(timerId);
	}

	public boolean isOustandingPeriodicTimer(long timerId) {
		return outstandingPeriodicTimers.contains(timerId);
	}

	public void handledTimerExpired(long timerId) {
		outstandingTimers.remove(timerId);
	}
}
