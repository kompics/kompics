package se.sics.kompics.timer.events;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

/**
 * 
 * @author Cosmin Arad
 * @version $Id:SetTimerEvent.java 24 2008-04-14 18:17:59Z cosmin $
 */
@EventType
public class SetTimerEvent implements Event {

	private Component clientComponent;

	private long delay;

	private long timerId;

	private TimerSignalEvent timerExpiredEvent;

	private Channel clientChannel;

	public SetTimerEvent(long timerId, TimerSignalEvent timerExpiredEvent,
			Channel channel, Component component, long delay) {
		this.clientChannel = channel;
		this.timerId = timerId;
		this.timerExpiredEvent = timerExpiredEvent;
		this.clientComponent = component;
		this.delay = delay;
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
}
