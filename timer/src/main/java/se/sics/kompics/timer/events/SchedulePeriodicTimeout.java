package se.sics.kompics.timer.events;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

/**
 * 
 * @author Cosmin Arad
 * @version $Id: SchedulePeriodicTimeout.java 24 2008-04-14 18:17:59Z cosmin $
 */
@EventType
public class SchedulePeriodicTimeout implements Event {

	private Component clientComponent;

	private long delay;

	private long period;

	private long timerId;

	private Timeout timeout;

	private Channel clientChannel;

	public SchedulePeriodicTimeout(long timerId, Timeout timeout,
			Channel channel, Component component, long delay, long period) {
		this.clientChannel = channel;
		this.timerId = timerId;
		this.timeout = timeout;
		this.clientComponent = component;
		this.delay = delay;
		this.period = period;
	}

	public Timeout getTimeout() {
		return timeout;
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
