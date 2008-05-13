package se.sics.kompics.timer;

import java.util.HashMap;
import java.util.Timer;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.Priority;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentDestroyMethod;
import se.sics.kompics.api.annotation.ComponentShareMethod;
import se.sics.kompics.api.annotation.ComponentType;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.timer.events.CancelPeriodicTimerEvent;
import se.sics.kompics.timer.events.CancelTimerEvent;
import se.sics.kompics.timer.events.SetPeriodicTimerEvent;
import se.sics.kompics.timer.events.SetTimerEvent;
import se.sics.kompics.timer.events.TimerSignalEvent;

/**
 * The <code>TimerComponent</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentType
public class TimerComponent {

	// set of active timers
	private HashMap<TimerId, TimerSignalTask> activeTimers;

	// set of active periodic timers
	private HashMap<TimerId, PeriodicTimerSignalTask> activePeriodicTimers;

	private Timer timer;

	private final Component component;

	private Channel requestChannel, signalChannel;

	/**
	 * Generates a timer component
	 */
	public TimerComponent(Component component) {
		this.component = component;
		this.activeTimers = new HashMap<TimerId, TimerSignalTask>();
		this.activePeriodicTimers = new HashMap<TimerId, PeriodicTimerSignalTask>();
	}

	@ComponentCreateMethod
	public void create(Channel requestChannel, Channel signalChannel) {
		this.requestChannel = requestChannel;
		this.signalChannel = signalChannel;

		// bind and subscribe to the given channels
		component.subscribe(requestChannel, "handleSetTimerEvent");
		component.subscribe(requestChannel, "handleSetPeriodicTimerEvent");
		component.subscribe(requestChannel, "handleCancelTimerEvent");
		component.subscribe(requestChannel, "handleCancelPeriodicTimerEvent");

		this.timer = new Timer("TimerComponent@"
				+ Integer.toHexString(this.hashCode()));
	}

	@ComponentShareMethod
	public ComponentMembrane share(String name) {
		HashMap<Class<? extends Event>, Channel> map = new HashMap<Class<? extends Event>, Channel>();
		map.put(SetTimerEvent.class, requestChannel);
		map.put(SetPeriodicTimerEvent.class, requestChannel);
		map.put(CancelTimerEvent.class, requestChannel);
		map.put(CancelPeriodicTimerEvent.class, requestChannel);
		map.put(TimerSignalEvent.class, signalChannel);
		ComponentMembrane membrane = new ComponentMembrane(component, map);
		return component.registerSharedComponentMembrane(name, membrane);
	}

	@ComponentDestroyMethod
	public void destroy() {
		this.timer.cancel();
		this.timer = null;
	}

	@EventHandlerMethod
	public void handleSetPeriodicTimerEvent(SetPeriodicTimerEvent event) {
		TimerId id = new TimerId(event.getClientComponent().getComponentUUID(),
				event.getTimerId());

		PeriodicTimerSignalTask timeOutTask = new PeriodicTimerSignalTask(
				component, event.getTimerExpiredEvent(), event
						.getClientChannel(), id);

		activePeriodicTimers.put(id, timeOutTask);
		timer.scheduleAtFixedRate(timeOutTask, event.getDelay(), event
				.getPeriod());
	}

	@EventHandlerMethod
	public void handleCancelPeriodicTimerEvent(CancelPeriodicTimerEvent event) {
		TimerId id = new TimerId(event.getClientComponent().getComponentUUID(),
				event.getTimerId());
		if (activePeriodicTimers.containsKey(id)) {
			activePeriodicTimers.get(id).cancel();
			activePeriodicTimers.remove(id);
		}
	}

	@EventHandlerMethod
	public void handleSetTimerEvent(SetTimerEvent event) {
		TimerId id = new TimerId(event.getClientComponent().getComponentUUID(),
				event.getTimerId());

		TimerSignalTask timeOutTask = new TimerSignalTask(this, event
				.getTimerExpiredEvent(), event.getClientChannel(), id);

		activeTimers.put(id, timeOutTask);
		timer.schedule(timeOutTask, event.getDelay());
	}

	@EventHandlerMethod
	public void handleCancelTimerEvent(CancelTimerEvent event) {
		TimerId id = new TimerId(event.getClientComponent().getComponentUUID(),
				event.getTimerId());

		if (activeTimers.containsKey(id)) {
			activeTimers.get(id).cancel();
			activeTimers.remove(id);
		}
	}

	// called by the timeout task
	void timeout(TimerId timerId, TimerSignalEvent timerExpiredEvent,
			Channel clientChannel) {
		activeTimers.remove(timerId);

		component.triggerEvent(timerExpiredEvent, clientChannel, Priority.HIGH);
	}
}
