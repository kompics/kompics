package se.sics.kompics.api;

import java.util.List;
import java.util.Set;

import se.sics.kompics.core.ComponentUUID;

public interface Component {

	public void triggerEvent(Event event, Channel channel);

	public void triggerEvent(Event event, Channel channel, Priority priority);

	/**
	 * Creates a new component instance.
	 * 
	 * @param faultChannel
	 *            the channel where the faults that occur in the newly created
	 *            component are reported as fault events.
	 * @param channelParameters
	 *            channel parameters of the new component.
	 * @return a reference to the newly created component.
	 */
	public Component createComponent(String componentClassName,
			Channel faultChannel, Channel... channelParameters);

	public void initialize(Object... objects);

	public Channel createChannel(Class<?>... eventTypes);

	public Channel getFaultChannel();

	// TODO document
	// TODO createFaultChannel
	public void subscribe(Channel channel, String eventHandlerName,
			EventAttributeFilter... filters);

	public void start();

	public void stop();

	public ComponentUUID getComponentUUID();

	/* =============== SHARING =============== */
	public ComponentMembrane getSharedComponentMembrane(String name);

	public ComponentMembrane registerSharedComponentMembrane(String name,
			ComponentMembrane membrane);

	public ComponentMembrane share(String name);

	/* =============== RECEIVE =============== */
	/**
	 * Wait to receive any event on any channel.
	 * 
	 * @return the received event.
	 */
	public Event receive();

	/**
	 * Wait to receive any event on any of the specified channels.
	 * 
	 * @param channels
	 *            the channels on which an event is awaited.
	 * @return the received event.
	 */
	public Event receive(Channel... channels);

	/**
	 * Wait to receive an event of the specified type on any of the specified
	 * channels.
	 * 
	 * @param eventType
	 *            the type of the awaited event.
	 * @param channels
	 *            the channels on which an event is awaited.
	 * @return the received event.
	 */
	public Event receive(Class<? extends Event> eventType, Channel... channels);

	/**
	 * Wait to receive an event of any of the specified types, on any of the
	 * specified channels.
	 * 
	 * @param eventTypes
	 *            the possible types of the awaited event.
	 * @param channels
	 *            the channels on which an event is awaited.
	 * @return the received event.
	 */
	public Event receive(Set<Class<? extends Event>> eventTypes,
			Channel... channels);

	/**
	 * Wait to receive an event of the specified type, that satisfies the
	 * specified event guard, on any of the specified channels.
	 * 
	 * @param eventType
	 *            the type of the awaited event.
	 * @param guardName
	 *            the name of the event guard method.
	 * @param channels
	 *            the channels on which an event is awaited.
	 * @return the received event.
	 */
	public Event receive(Class<? extends Event> eventType, String guardName,
			Channel... channels);

	/* =============== COMPONENT COMPOSITION =============== */

	public List<Component> getSubComponents();

	public List<Channel> getLocalChannels();

	public Component getSuperComponent();

	public String getName();
}
