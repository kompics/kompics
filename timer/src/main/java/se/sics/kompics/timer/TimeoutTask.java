package se.sics.kompics.timer;

import java.util.TimerTask;

import se.sics.kompics.api.Channel;
import se.sics.kompics.timer.events.TimerExpiredEvent;

/**
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
class TimeoutTask extends TimerTask {

	private TimerExpiredEvent timerExpiredEvent;

	private TimerId timerId;

	private TimerComponent timerComponent;

	private Channel clientChannel;

	TimeoutTask(TimerComponent timerComponent,
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
		timerComponent.timeout(timerId, timerExpiredEvent, clientChannel);
	}
}
