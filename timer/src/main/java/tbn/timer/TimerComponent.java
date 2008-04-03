package tbn.timer;

import java.util.HashMap;
import java.util.Timer;

import org.apache.log4j.Logger;

import tbn.api.Component;
import tbn.api.ComponentCannotBeStoppedException;
import tbn.api.Priority;
import tbn.timer.events.TimerEvent;
import tbn.timer.events.TimerExpiredEvent;

/**
 * The <code>TimerComponent</code> class
 * 
 * @author Roberto Roverso
 * @author Cosmin Arad
 * @version $Id$
 */
public class TimerComponent {

	private static Logger log = Logger.getLogger(TimerComponent.class);

	/* Map keeping track of the Timers now in execution */
	// private HashSet<TimerInfo> timers;
	private HashMap<String, TimeOutTask> timersMap;

	private Timer timer;

	private Component component;

	/**
	 * Generates a timer component
	 */
	public TimerComponent(Component component) {
		this.component = component;
		this.timer = new Timer();
		this.timersMap = new HashMap<String, TimeOutTask>();
	}

	public void handleTimerEvent(TimerEvent event) {

		if (event.getAction().equals(TimerEvent.TimerAction.START)) {

			String key = event.getComponent().getName() + "%"
					+ event.getTimerId();

			TimeOutTask timeOutTask = new TimeOutTask(event
					.getTimerExpiredEvent(), this, event.getTimerId(), event
					.getComponent(), key);

			log.debug("Setting timer num: " + event.getTimerId()
					+ " of component " + event.getComponent().getName()
					+ " for event " + event.getTimerExpiredEvent());

			timersMap.put(key, timeOutTask);

			timer.schedule(timeOutTask, event.getTimeout());

		} else {
			cancelTimer(event.getTimerId(), event.getComponent());
		}
	}

	public void cancelTimer(long timerId, Component component) {

		String key = component.getName() + "%" + timerId;

		if (timersMap.containsKey(key)) {

			timersMap.get(key).cancel();

			log.debug("Removing timer num: " + timerId + " for component "
					+ component.getName());
		} else {
			log.debug("Impossible to remove timer num: " + timerId
					+ " for component " + component.getName());
		}
	}

	/**
	 * Stops all the timers
	 */
	public void stop() {
		if (!timersMap.isEmpty()) {
			throw new ComponentCannotBeStoppedException(TimerComponent.class
					.getCanonicalName());
		}
	}

	public void timeout(String key, TimerExpiredEvent timerExpiredEvent) {
		timersMap.remove(key);
		component.raiseEvent(timerExpiredEvent, Priority.HIGH);
	}
}
