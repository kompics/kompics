package se.sics.kompics.core;

import java.util.HashMap;
import java.util.HashSet;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.Priority;
import se.sics.kompics.core.config.ConfigurationException;
import se.sics.kompics.core.sched.Work;

public class ComponentCore implements Component {

	/**
	 * reference to the component instance implementing the component
	 * functionality, i.e., state and event handlers
	 */
	private Object behaviour;

	/* =============== COMPOSITION =============== */

	/**
	 * internal sub-components
	 */
	private HashSet<ComponentCore> subcomponents;

	/**
	 * internal channels
	 */
	private HashSet<ChannelCore> subchannels;

	/* =============== CONFIGURATION =============== */

	private HashMap<Class<? extends Event>, Binding> bindings;

	private HashMap<Class<? extends Event>, Subscription> subscriptions;

	/* =============== SCHEDULING =============== */

	// private HashMap<ChannelCore, Queue<EventCore>>
	public ComponentCore() {
		super();
		bindings = new HashMap<Class<? extends Event>, Binding>();
	}

	public void setBehaviour(Object behaviour) {
		this.behaviour = behaviour;
	}

	/* =============== EVENT TRIGGERING =============== */

	/**
	 * triggers an event
	 * 
	 * @param event
	 *            the triggered event
	 */
	public void triggerEvent(Event event) {
		Binding binding = bindings.get(event.getClass());

		if (binding == null)
			throw new ConfigurationException("Event type "
					+ event.getClass().getCanonicalName() + " not bound");

		EventCore eventCore = new EventCore(event, binding.getChannel(),
				Priority.NORMAL);
		triggerEventCore(eventCore);
	}

	public void triggerEvent(Event event, Priority priority) {
		Binding binding = bindings.get(event.getClass());

		if (binding == null)
			throw new ConfigurationException("Event type "
					+ event.getClass().getCanonicalName() + " not bound");

		EventCore eventCore = new EventCore(event, binding.getChannel(),
				priority);
		triggerEventCore(eventCore);
	}

	public void triggerEvent(Event event, ChannelCore channel) {
		EventCore eventCore = new EventCore(event, channel, Priority.NORMAL);
		triggerEventCore(eventCore);
	}

	public void triggerEvent(Event event, ChannelCore channel, Priority priority) {
		EventCore eventCore = new EventCore(event, channel, priority);
		triggerEventCore(eventCore);
	}

	private void triggerEventCore(EventCore eventCore) {
		ChannelCore channelCore = eventCore.getChannelCore();
		channelCore.publishEventCore(eventCore);
	}

	/* =============== SCHEDULING =============== */

	public void handleWork(Work work) {

	}

	public void schedule(Priority priority) {

	}

	/* =============== CONFIGURATION =============== */
}
