package se.sics.kompics.timer;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.EventHandler;
import se.sics.kompics.api.Priority;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentDestroyMethod;
import se.sics.kompics.api.annotation.ComponentShareMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.timer.events.CancelPeriodicTimeout;
import se.sics.kompics.timer.events.CancelTimeout;
import se.sics.kompics.timer.events.SchedulePeriodicTimeout;
import se.sics.kompics.timer.events.ScheduleTimeout;
import se.sics.kompics.timer.events.Timeout;

/**
 * The <code>Timer</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class Timer {

	private static final Logger logger = LoggerFactory.getLogger(Timer.class);

	// set of active timers
	private HashMap<TimerId, TimerSignalTask> activeTimers;

	// set of active periodic timers
	private HashMap<TimerId, PeriodicTimerSignalTask> activePeriodicTimers;

	private java.util.Timer timer;

	private final Component component;

	private final Timer thisTimer;

	private Channel requestChannel, signalChannel;

	/**
	 * Generates a timer component
	 */
	public Timer(Component component) {
		this.component = component;
		this.activeTimers = new HashMap<TimerId, TimerSignalTask>();
		this.activePeriodicTimers = new HashMap<TimerId, PeriodicTimerSignalTask>();
		thisTimer = this;
	}

	@ComponentCreateMethod
	public void create(Channel requestChannel, Channel signalChannel) {
		this.requestChannel = requestChannel;
		this.signalChannel = signalChannel;

		// subscribe to the given channels
		component.subscribe(requestChannel, handleScheduleTimeout);
		component.subscribe(requestChannel, handleSchedulePeriodicTimeout);
		component.subscribe(requestChannel, handleCancelTimeout);
		component.subscribe(requestChannel, handleCancelPeriodicTimeout);

		this.timer = new java.util.Timer("TimerComponent@"
				+ Integer.toHexString(this.hashCode()));
	}

	@ComponentShareMethod
	public ComponentMembrane share(String name) {
		ComponentMembrane membrane = new ComponentMembrane(component);
		membrane.inChannel(ScheduleTimeout.class, requestChannel);
		membrane.inChannel(SchedulePeriodicTimeout.class, requestChannel);
		membrane.inChannel(CancelTimeout.class, requestChannel);
		membrane.inChannel(CancelPeriodicTimeout.class, requestChannel);
		membrane.outChannel(Timeout.class, signalChannel);
		membrane.seal();
		return component.registerSharedComponentMembrane(name, membrane);
	}

	@ComponentDestroyMethod
	public void destroy() {
		this.timer.cancel();
		this.timer = null;
	}

	private EventHandler<SchedulePeriodicTimeout> handleSchedulePeriodicTimeout = new EventHandler<SchedulePeriodicTimeout>() {
		public void handle(SchedulePeriodicTimeout event) {
			TimerId id = new TimerId(event.getClientComponent()
					.getComponentUUID(), event.getTimerId());

			PeriodicTimerSignalTask timeOutTask = new PeriodicTimerSignalTask(
					component, event.getTimeout(), event.getClientChannel(), id);

			activePeriodicTimers.put(id, timeOutTask);
			timer.scheduleAtFixedRate(timeOutTask, event.getDelay(), event
					.getPeriod());
		}
	};

	private EventHandler<CancelPeriodicTimeout> handleCancelPeriodicTimeout = new EventHandler<CancelPeriodicTimeout>() {
		public void handle(CancelPeriodicTimeout event) {
			TimerId id = new TimerId(event.getClientComponent()
					.getComponentUUID(), event.getTimerId());
			if (activePeriodicTimers.containsKey(id)) {
				activePeriodicTimers.get(id).cancel();
				activePeriodicTimers.remove(id);
			}
		}
	};

	private EventHandler<ScheduleTimeout> handleScheduleTimeout = new EventHandler<ScheduleTimeout>() {
		public void handle(ScheduleTimeout event) {
			logger.debug("Handling SET TIMER: ", event);

			TimerId id = new TimerId(event.getClientComponent()
					.getComponentUUID(), event.getTimerId());

			TimerSignalTask timeOutTask = new TimerSignalTask(thisTimer, event
					.getTimeout(), event.getClientChannel(), id);

			synchronized (activeTimers) {
				activeTimers.put(id, timeOutTask);
			}
			timer.schedule(timeOutTask, event.getDelay());
		}
	};

	private EventHandler<CancelTimeout> handleCancelTimeout = new EventHandler<CancelTimeout>() {
		public void handle(CancelTimeout event) {
			TimerId id = new TimerId(event.getClientComponent()
					.getComponentUUID(), event.getTimerId());

			synchronized (activeTimers) {
				if (activeTimers.containsKey(id)) {
					activeTimers.get(id).cancel();
					activeTimers.remove(id);
				}
			}
		}
	};

	// called by the timeout task
	void timeout(TimerId timerId, Timeout timerExpiredEvent,
			Channel clientChannel) {
		synchronized (activeTimers) {
			activeTimers.remove(timerId);
		}

		logger.debug("TRIGGERing timer expired {}", timerExpiredEvent);

		component.triggerEvent(timerExpiredEvent, clientChannel, Priority.HIGH);
	}
}
