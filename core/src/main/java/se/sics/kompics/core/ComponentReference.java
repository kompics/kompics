package se.sics.kompics.core;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Set;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.Priority;
import se.sics.kompics.api.capability.CapabilityNotSupportedException;
import se.sics.kompics.api.capability.ComponentCapabilityFlags;
import se.sics.kompics.core.scheduler.Work;

public class ComponentReference implements Component {

	/**
	 * The referenced component's identifier.
	 */
	private final ComponentUUID componentUUID;

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
	private final ComponentUUID capable;

	/**
	 * Constructs a component reference. This constructor is only visible in the
	 * se.sics.kompics.core package.
	 * 
	 * @param componentCore
	 * @param componentCapabilities
	 */
	ComponentReference(ComponentCore componentCore,
			ComponentUUID componentUUID,
			EnumSet<ComponentCapabilityFlags> componentCapabilities) {
		super();

		if (componentCore == null) {
			throw new RuntimeException(
					"Cannot create a component reference to a null component.");
		}

		this.componentCore = componentCore;
		this.componentUUID = componentUUID;

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
			ComponentUUID componentUUID,
			EnumSet<ComponentCapabilityFlags> componentCapabilities,
			ComponentUUID capable) {
		super();

		if (componentCore == null) {
			throw new RuntimeException(
					"Cannot create a component reference to a null component.");
		}

		this.componentCore = componentCore;
		this.componentUUID = componentUUID;

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

	public Channel getFaultChannel() {
		// TODO create channel capability
		return componentCore.getFaultChannel();
	}

	public Channel createChannel(Class<?>... eventTypes) {
		// TODO create channel capability
		return componentCore.createChannel(eventTypes);
	}

	public Component createComponent(String componentClassName,
			Channel faultChannel, Channel... channelParameters) {
		// TODO create component capability
		return componentCore.createComponent(componentClassName, faultChannel,
				channelParameters);
	}

	public void initialize(Object... objects) {
		componentCore.initialize(objects);
		// TODO init capability
	}

	public void start() {
		if (componentCapabilities.contains(ComponentCapabilityFlags.START)
				&& (capable == null || capable.equals(ComponentUUID.get()))) {
			componentCore.start();
			return;
		}
		throw new CapabilityNotSupportedException(
				ComponentCapabilityFlags.START);
	}

	public void stop() {
		if (componentCapabilities.contains(ComponentCapabilityFlags.STOP)
				&& (capable == null || capable.equals(ComponentUUID.get()))) {
			componentCore.stop();
			return;
		}
		throw new CapabilityNotSupportedException(ComponentCapabilityFlags.STOP);
	}

	public void subscribe(Channel channel, String eventHandlerName) {
		if (componentCapabilities.contains(ComponentCapabilityFlags.SUBSCRIBE)
				&& (capable == null || capable.equals(ComponentUUID.get()))) {
			componentCore.subscribe(this, channel, eventHandlerName);
			return;
		}
		throw new CapabilityNotSupportedException(
				ComponentCapabilityFlags.SUBSCRIBE);
	}

	public void triggerEvent(Event event, Channel channel) {
		if (componentCapabilities.contains(ComponentCapabilityFlags.TRIGGER)
				&& (capable == null || capable.equals(ComponentUUID.get()))) {
			componentCore.triggerEvent(event, channel);
			return;
		}
		throw new CapabilityNotSupportedException(
				ComponentCapabilityFlags.TRIGGER);
	}

	public void triggerEvent(Event event, Channel channel, Priority priority) {
		if (componentCapabilities.contains(ComponentCapabilityFlags.TRIGGER)
				&& (capable == null || capable.equals(ComponentUUID.get()))) {
			componentCore.triggerEvent(event, channel, priority);
			return;
		}
		throw new CapabilityNotSupportedException(
				ComponentCapabilityFlags.TRIGGER);
	}

	void handleWork(Work work) {
		if (componentCapabilities
				.contains(ComponentCapabilityFlags.HANDLE_EVENTS)
				&& (capable == null || capable.equals(ComponentUUID.get()))) {
			componentCore.handleWork(work);
			return;
		}
		throw new CapabilityNotSupportedException(
				ComponentCapabilityFlags.HANDLE_EVENTS);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((componentUUID == null) ? 0 : componentUUID.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ComponentReference other = (ComponentReference) obj;
		if (componentUUID == null) {
			if (other.componentUUID != null)
				return false;
		} else if (!componentUUID.equals(other.componentUUID))
			return false;
		return true;
	}

	public ComponentUUID getComponentUUID() {
		return componentUUID;
	}

	/* =============== RECEIVE =============== */
	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.kompics.api.Component#receive()
	 */
	public Event receive() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.kompics.api.Component#receive(se.sics.kompics.api.Channel[])
	 */
	public Event receive(Channel... channels) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.kompics.api.Component#receive(java.lang.Class,
	 * se.sics.kompics.api.Channel[])
	 */
	public Event receive(Class<? extends Event> eventType, Channel... channels) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.kompics.api.Component#receive(java.util.Set,
	 * se.sics.kompics.api.Channel[])
	 */
	public Event receive(Set<Class<? extends Event>> eventTypes,
			Channel... channels) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.kompics.api.Component#receive(java.lang.Class,
	 * java.lang.String, se.sics.kompics.api.Channel[])
	 */
	public Event receive(Class<? extends Event> eventType, String guardName,
			Channel... channels) {
		// TODO Auto-generated method stub
		return null;
	}

	/* =============== SHARING =============== */
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * se.sics.kompics.api.Component#getSharedComponentMembrane(java.lang.String
	 * )
	 */
	public ComponentMembrane getSharedComponentMembrane(String name) {
		return componentCore.getSharedComponentMembrane(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.kompics.api.Component#share(java.lang.String)
	 */
	public ComponentMembrane share(String name) {
		return componentCore.share(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * se.sics.kompics.api.Component#registerSharedComponentMembrane(java.lang
	 * .String, se.sics.kompics.api.ComponentMembrane)
	 */
	public ComponentMembrane registerSharedComponentMembrane(String name,
			ComponentMembrane membrane) {
		return componentCore.registerSharedComponentMembrane(name, membrane);
	}

	/* =============== COMPONENT COMPOSITION =============== */

	public LinkedList<Component> getSubComponents() {
		// TODO navigation capability
		return componentCore.getSubComponents();
	}

	public LinkedList<Channel> getLocalChannels() {
		return componentCore.getLocalChannels();
	}

	public Component getSuperComponent() {
		return componentCore.getSuperComponent();
	}

	public String getName() {
		return componentCore.getName();
	}
}
