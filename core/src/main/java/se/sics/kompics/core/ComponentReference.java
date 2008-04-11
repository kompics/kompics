package se.sics.kompics.core;

import java.util.EnumSet;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.Factory;
import se.sics.kompics.api.Priority;
import se.sics.kompics.api.capability.ComponentCapabilityFlags;
import se.sics.kompics.core.scheduler.Work;

public class ComponentReference implements Component {

	/**
	 * The component referenced by this component reference.
	 */
	private ComponentCore componentCore;

	/**
	 * Set of capabilities offered on the referenced component.
	 */
	private EnumSet<ComponentCapabilityFlags> componentCapabilities;

	/**
	 * The identifier of the component that is capable to use this reference. If
	 * <code>null</code> any component is capable to use this reference.
	 */
	private ComponentIdentifier capable;

	/**
	 * Constructs a component reference. This constructor is only visible in the
	 * se.sics.kompics.core package.
	 * 
	 * @param componentCore
	 * @param componentCapabilities
	 */
	ComponentReference(ComponentCore componentCore,
			EnumSet<ComponentCapabilityFlags> componentCapabilities) {
		super();

		if (componentCore == null) {
			throw new RuntimeException(
					"Cannot create a component reference to a null component.");
		}

		this.componentCore = componentCore;

		if (componentCapabilities == null) {
			this.componentCapabilities = EnumSet
					.noneOf(ComponentCapabilityFlags.class);
		} else {
			this.componentCapabilities = componentCapabilities;
		}
	}

	/**
	 * Constructs a component reference. This constructor is visible only in the
	 * se.sics.kompics.core package.
	 * 
	 * @param componentCore
	 * @param componentCapabilities
	 * @param capable
	 */
	ComponentReference(ComponentCore componentCore,
			EnumSet<ComponentCapabilityFlags> componentCapabilities,
			ComponentIdentifier capable) {
		this(componentCore, componentCapabilities);
		this.capable = capable;
	}

	void revokeCapability() {
		;
	}

	public void bind(Class<? extends Event> eventType, Channel channel) {
		// TODO Auto-generated method stub
		componentCore.bind(this, eventType, channel);
	}

	public Channel createChannel() {
		// TODO Auto-generated method stub
		return componentCore.createChannel();
	}

	public Factory createFactory(String componentClassName)
			throws ClassNotFoundException {
		// TODO Auto-generated method stub
		return componentCore.createFactory(componentClassName);
	}

	public void start() {
		// TODO Auto-generated method stub
		componentCore.start();
	}

	public void stop() {
		// TODO Auto-generated method stub
		componentCore.stop();
	}

	public void subscribe(Channel channel, String eventHandlerName) {
		// TODO Auto-generated method stub
		componentCore.subscribe(this, channel, eventHandlerName);
	}

	public void triggerEvent(Event event) {
		// TODO Auto-generated method stub
		componentCore.triggerEvent(event);
	}

	public void triggerEvent(Event event, Priority priority) {
		// TODO Auto-generated method stub
		componentCore.triggerEvent(event, priority);
	}

	public void triggerEvent(Event event, Channel channel) {
		// TODO Auto-generated method stub
		componentCore.triggerEvent(event, channel);
	}

	public void triggerEvent(Event event, Channel channel, Priority priority) {
		// TODO Auto-generated method stub
		componentCore.triggerEvent(event, channel, priority);
	}

	void handleWork(Work work) {
		// TODO capability eventHandling
		componentCore.handleWork(work);
	}
}
