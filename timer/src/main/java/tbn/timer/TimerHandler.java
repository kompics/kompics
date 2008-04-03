package tbn.timer;

import java.util.HashSet;

import org.apache.log4j.Logger;

import tbn.api.Channel;
import tbn.api.Component;
import tbn.api.HandlerNotSubscribedException;
import tbn.api.NoSuchMethodException;
import tbn.api.Priority;
import tbn.core.util.LongSequenceGenerator;
import tbn.timer.events.TimerEvent;
import tbn.timer.events.TimerExpiredEvent;

public class TimerHandler {

	private static Logger log = Logger.getLogger(TimerHandler.class);

	private Component component;

	private LongSequenceGenerator timerIdGenerator;

	private HashSet<Long> outstandingTimers;

	public TimerHandler(Component component) {
		super();
		this.component = component;
		this.timerIdGenerator = new LongSequenceGenerator(0);
		this.outstandingTimers = new HashSet<Long>();
	}

	public long startTimer(TimerExpiredEvent timerExpiredEvent,
			String eventHandlerName, long timeout)
			throws HandlerNotSubscribedException, NoSuchMethodException {

		Channel channel = component
				.getChannelForSubscribedHandler(eventHandlerName);

		log.debug("CHANNEL " + channel.getName());

		long timerId = timerIdGenerator.getNextSequenceNumber();

		TimerEvent event = new TimerEvent(timerId, timerExpiredEvent, channel,
				component, eventHandlerName, timeout);

		outstandingTimers.add(timerId);
		component.raiseEvent(event, Priority.HIGH);
		return timerId;
	}

	public void stopTimer(long timerId) {

		TimerEvent event = new TimerEvent(component, timerId);

		component.raiseEvent(event, Priority.HIGH);
		outstandingTimers.remove(timerId);
	}

	public boolean isOustanding(long timerId) {
		return outstandingTimers.contains(timerId);
	}

	public void handledTimer(long timerId) {
		outstandingTimers.remove(timerId);
	}
}
