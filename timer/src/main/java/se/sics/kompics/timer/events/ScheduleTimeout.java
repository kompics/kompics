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
public class ScheduleTimeout implements Event {

	private Component clientComponent;

	private long delay;

	private long timerId;

	private Timeout timeout;

	private Channel clientChannel;

	public ScheduleTimeout(long timerId, Timeout timeout, Channel channel,
			Component component, long delay) {
		this.clientChannel = channel;
		this.timerId = timerId;
		this.timeout = timeout;
		this.clientComponent = component;
		this.delay = delay;
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
}
