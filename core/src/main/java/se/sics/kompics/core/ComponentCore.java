package se.sics.kompics.core;

import java.util.HashSet;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.Priority;

public class ComponentCore implements Component {

	/**
	 * internal sub-components
	 */
	private HashSet<ComponentCore> subcomponents;

	/**
	 * internal channels
	 */
	private HashSet<ChannelCore> subchannels;

	/**
	 * reference to the component instance implementing the component
	 * functionality, i.e., state and event handlers
	 */
	private Object behaviour;

	/**
	 * s
	 * 
	 * @param event
	 *            the triggered event
	 */
	public void triggerEvent(Event event) {
		
	}

	public void schedule(Priority priority) {

	}
}
