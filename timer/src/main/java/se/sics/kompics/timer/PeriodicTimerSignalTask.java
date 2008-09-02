package se.sics.kompics.timer;

import java.util.TimerTask;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Priority;
import se.sics.kompics.timer.events.Timeout;

/**
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
class PeriodicTimerSignalTask extends TimerTask {

	private Timeout timerExpiredEvent;

	private TimerId timerId;

	private Component timerComponent;

	private Channel clientChannel;

	PeriodicTimerSignalTask(Component timerComponent,
			Timeout timerExpiredEvent, Channel clientChannel,
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
