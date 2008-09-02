package se.sics.kompics.timer;

import java.util.TimerTask;

import se.sics.kompics.api.Channel;
import se.sics.kompics.timer.events.Timeout;

/**
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
class TimerSignalTask extends TimerTask {

	private Timeout timerExpiredEvent;

	private TimerId timerId;

	private Timer timerComponent;

	private Channel clientChannel;

	TimerSignalTask(Timer timerComponent,
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
		timerComponent.timeout(timerId, timerExpiredEvent, clientChannel);
	}
}
