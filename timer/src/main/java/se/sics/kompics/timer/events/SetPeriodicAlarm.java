package se.sics.kompics.timer.events;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

/**
 * 
 * @author Cosmin Arad
 * @version $Id: SetPeriodicAlarm.java 24 2008-04-14 18:17:59Z cosmin $
 */
@EventType
public class SetPeriodicAlarm implements Event {

	private Component clientComponent;

	private long delay;

	private long period;

	private long timerId;

	private Alarm timerExpiredEvent;

	private Channel clientChannel;

	public SetPeriodicAlarm(long timerId, Alarm timerExpiredEvent,
			Channel channel, Component component, long delay, long period) {
		this.clientChannel = channel;
		this.timerId = timerId;
		this.timerExpiredEvent = timerExpiredEvent;
		this.clientComponent = component;
		this.delay = delay;
		this.period = period;
	}

	public Alarm getTimerExpiredEvent() {
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
