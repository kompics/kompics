package tbn.timer;

import java.util.TimerTask;

import org.apache.log4j.Logger;

import tbn.api.Component;
import tbn.timer.events.TimerExpiredEvent;

/**
 * The <code>TimeOutTask</code> class
 * 
 * @author Roberto Roverso
 * @author Cosmin Arad
 * @version $Id$
 */
public class TimeOutTask extends TimerTask {

	private static Logger log = Logger.getLogger(TimeOutTask.class);

	private TimerComponent timer;

	private TimerExpiredEvent timerExpiredEvent;

	private long timerID;

	private Component component;

	private String key;

	/**
	 * Generate a TimeOutTask to be triggered by the Timer when the amount of
	 * time set is expired
	 * 
	 * @param eventClass
	 * @param component
	 */
	public TimeOutTask(TimerExpiredEvent timerExpiredEvent, TimerComponent timer,
			long timerID, Component component, String key) {
		this.timer = timer;
		this.timerExpiredEvent = timerExpiredEvent;
		this.timerID = timerID;
		this.component = component;
		this.key = key;
	}

	@Override
	public void run() {
		log.debug("Timeout " + timerID + " of component " + component.getName()
				+ " expired - issuing event " + timerExpiredEvent);

		timerExpiredEvent.setTimerId(timerID);
		timer.timeout(key, timerExpiredEvent);
	}
}
