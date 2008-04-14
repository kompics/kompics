package se.sics.kompics.core;

import java.util.EnumSet;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.Factory;
import se.sics.kompics.api.Priority;
import se.sics.kompics.api.capability.CapabilityNotSupportedException;
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
	private final EnumSet<ComponentCapabilityFlags> componentCapabilities;

	/**
	 * The identifier of the component that is capable to use this reference. If
	 * <code>null</code> any component is capable to use this reference.
	 */
	private final ComponentIdentifier capable;

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
		this.capable = null;
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
		this.capable = capable;
	}

	void revoke() {
		componentCore = null;
	}

	public void bind(Class<? extends Event> eventType, Channel channel) {
		if (componentCapabilities.contains(ComponentCapabilityFlags.BIND)
				&& (capable == null || capable
						.equals(ComponentIdentifier.get()))) {
			componentCore.bind(this, eventType, channel);
			return;
		}
		throw new CapabilityNotSupportedException(ComponentCapabilityFlags.BIND);
	}

	public Channel createChannel() {
		// TODO create channel capability
		return componentCore.createChannel();
	}

	public Factory createFactory(String componentClassName)
			throws ClassNotFoundException {
		// TODO create factory capability
		return componentCore.createFactory(componentClassName);
	}

	public void start() {
		if (componentCapabilities.contains(ComponentCapabilityFlags.START)
				&& (capable == null || capable
						.equals(ComponentIdentifier.get()))) {
			componentCore.start();
			return;
		}
		throw new CapabilityNotSupportedException(
				ComponentCapabilityFlags.START);
	}

	public void stop() {
		if (componentCapabilities.contains(ComponentCapabilityFlags.STOP)
				&& (capable == null || capable
						.equals(ComponentIdentifier.get()))) {
			componentCore.stop();
			return;
		}
		throw new CapabilityNotSupportedException(ComponentCapabilityFlags.STOP);
	}

	public void subscribe(Channel channel, String eventHandlerName) {
		if (componentCapabilities.contains(ComponentCapabilityFlags.SUBSCRIBE)
				&& (capable == null || capable
						.equals(ComponentIdentifier.get()))) {
			componentCore.subscribe(this, channel, eventHandlerName);
			return;
		}
		throw new CapabilityNotSupportedException(
				ComponentCapabilityFlags.SUBSCRIBE);
	}

	public void triggerEvent(Event event) {
		if (componentCapabilities.contains(ComponentCapabilityFlags.TRIGGER)
				&& (capable == null || capable
						.equals(ComponentIdentifier.get()))) {
			componentCore.triggerEvent(event);
			return;
		}
		throw new CapabilityNotSupportedException(
				ComponentCapabilityFlags.TRIGGER);
	}

	public void triggerEvent(Event event, Priority priority) {
		if (componentCapabilities.contains(ComponentCapabilityFlags.TRIGGER)
				&& (capable == null || capable
						.equals(ComponentIdentifier.get()))) {
			componentCore.triggerEvent(event, priority);
			return;
		}
		throw new CapabilityNotSupportedException(
				ComponentCapabilityFlags.TRIGGER);
	}

	public void triggerEvent(Event event, Channel channel) {
		if (componentCapabilities.contains(ComponentCapabilityFlags.TRIGGER)
				&& (capable == null || capable
						.equals(ComponentIdentifier.get()))) {
			componentCore.triggerEvent(event, channel);
			return;
		}
		throw new CapabilityNotSupportedException(
				ComponentCapabilityFlags.TRIGGER);
	}

	public void triggerEvent(Event event, Channel channel, Priority priority) {
		if (componentCapabilities.contains(ComponentCapabilityFlags.TRIGGER)
				&& (capable == null || capable
						.equals(ComponentIdentifier.get()))) {
			componentCore.triggerEvent(event, channel, priority);
			return;
		}
		throw new CapabilityNotSupportedException(
				ComponentCapabilityFlags.TRIGGER);
	}

	void handleWork(Work work) {
		if (componentCapabilities
				.contains(ComponentCapabilityFlags.HANDLE_EVENTS)
				&& (capable == null || capable
						.equals(ComponentIdentifier.get()))) {
			componentCore.handleWork(work);
			return;
		}
		throw new CapabilityNotSupportedException(
				ComponentCapabilityFlags.HANDLE_EVENTS);
	}
}
