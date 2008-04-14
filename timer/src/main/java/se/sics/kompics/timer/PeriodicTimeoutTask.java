package se.sics.kompics.timer;

import java.util.TimerTask;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Priority;
import se.sics.kompics.timer.events.TimerExpiredEvent;

/**
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
class PeriodicTimeoutTask extends TimerTask {

	private TimerExpiredEvent timerExpiredEvent;

	private TimerId timerId;

	private Component timerComponent;

	private Channel clientChannel;

	PeriodicTimeoutTask(Component timerComponent,
			TimerExpiredEvent timerExpiredEvent, Channel clientChannel,
			TimerId timerId) {
		super();
		this.timerComponent = timerComponent;
		this.timerExpiredEvent = timerExpiredEvent;
		this.clientChannel = clientChannel;
		this.timerId = timerId;
	}

	@Override
	public void run() {
		timerExpiredEvent.setTimerId(timerId.getId());
		timerComponent.triggerEvent(timerExpiredEvent, clientChannel,
				Priority.HIGH);
	}
}
