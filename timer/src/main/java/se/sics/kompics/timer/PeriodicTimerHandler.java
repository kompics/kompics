package se.sics.kompics.timer;

import java.util.HashSet;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Priority;
import se.sics.kompics.timer.events.CancelPeriodicAlarm;
import se.sics.kompics.timer.events.SetPeriodicAlarm;
import se.sics.kompics.timer.events.Alarm;

/**
 * The <code>PeriodicTimerHandler</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
public class PeriodicTimerHandler {

	private final Component component;

	private final Channel setTimerChannel;

	private long nextPeriodicTimerId;

	private final HashSet<Long> outstandingPeriodicTimers;

	public PeriodicTimerHandler(Component component, Channel setTimerChannel) {
		super();
		this.component = component;
		this.setTimerChannel = setTimerChannel;
		this.nextPeriodicTimerId = 0;
		this.outstandingPeriodicTimers = new HashSet<Long>();
	}

	public long setPeriodicTimer(Alarm timerExpiredEvent,
			Channel timeoutChannel, long delay, long period) {
		SetPeriodicAlarm event = new SetPeriodicAlarm(
				++nextPeriodicTimerId, timerExpiredEvent, timeoutChannel,
				component, delay, period);

		outstandingPeriodicTimers.add(nextPeriodicTimerId);
		component.triggerEvent(event, setTimerChannel, Priority.HIGH);
		return nextPeriodicTimerId;
	}

	public void cancelPeriodicTimer(long timerId) {
		CancelPeriodicAlarm event = new CancelPeriodicAlarm(
				component, timerId);
		component.triggerEvent(event, setTimerChannel, Priority.HIGH);
		outstandingPeriodicTimers.remove(timerId);
	}

	public boolean isOustandingPeriodicTimer(long timerId) {
		return outstandingPeriodicTimers.contains(timerId);
	}
}
