package tbn.timer.events;

import tbn.api.Channel;
import tbn.api.Component;
import tbn.api.Event;

public class TimerEvent implements Event {

	public static enum TimerAction {
		START, STOP
	};

	private TimerAction action;

	private TimerExpiredEvent timerExpiredEvent;

	private Component component;

	private String eventHandlerName;

	private long timeout;

	private long timerId;

	private Channel destinationChannel;

	/**
	 * Constructs a TimerEvent for starting a timer
	 * 
	 * @param eventClass
	 * @param component
	 * @param eventHandlerName
	 * @param obj
	 * @param timeout
	 */
	public TimerEvent(long timerId, TimerExpiredEvent timerExpiredEvent,
			Channel channel, Component component, String eventHandlerName,
			long timeout) {

		this.destinationChannel = channel;
		this.timerId = timerId;
		this.action = TimerAction.START;
		this.timerExpiredEvent = timerExpiredEvent;
		this.component = component;
		this.eventHandlerName = eventHandlerName;
		this.timeout = timeout;
	}

	/**
	 * Constructs a TimerEvent for stopping a timer
	 * 
	 * @param timerId
	 */
	public TimerEvent(Component component, long timerId) {
		this.action = TimerAction.STOP;
		this.component = component;
		this.timerId = timerId;
	}

	public TimerAction getAction() {
		return action;
	}

	public TimerExpiredEvent getTimerExpiredEvent() {
		return timerExpiredEvent;
	}

	public Component getComponent() {
		return component;
	}

	public String getEventHandlerName() {
		return eventHandlerName;
	}

	public long getTimeout() {
		return timeout;
	}

	public long getTimerId() {
		return timerId;
	}

	public Channel getDestinationChannel() {
		return destinationChannel;
	}

}
