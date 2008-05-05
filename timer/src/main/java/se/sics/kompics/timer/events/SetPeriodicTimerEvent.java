package se.sics.kompics.timer.events;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

/**
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public class SetPeriodicTimerEvent implements Event {

	private Component clientComponent;

	private long delay;

	private long period;

	private long timerId;

	private TimerSignalEvent timerExpiredEvent;

	private Channel clientChannel;

	public SetPeriodicTimerEvent(long timerId,
			TimerSignalEvent timerExpiredEvent, Channel channel,
			Component component, long delay, long period) {
		this.clientChannel = channel;
		this.timerId = timerId;
		this.timerExpiredEvent = timerExpiredEvent;
		this.clientComponent = component;
		this.delay = delay;
		this.period = period;
	}

	public TimerSignalEvent getTimerExpiredEvent() {
		return timerExpiredEvent;
	}

	public Component getClientComponent() {
		return clientComponent;
	}

	public long getDelay() {
		return delay;
	}

	public long getTimerId() {
		return timerId;
	}

	public Channel getClientChannel() {
		return clientChannel;
	}

	public long getPeriod() {
		return period;
	}
}
