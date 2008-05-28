package se.sics.kompics.api;

import java.util.Set;

import se.sics.kompics.core.ComponentUUID;

public interface Component {

	public void triggerEvent(Event event, Channel channel);

	public void triggerEvent(Event event, Channel channel, Priority priority);

	/**
	 * Creates a component factory.
	 * 
	 * @param componentClassName
	 *            the name of the class that contains the implementation of the
	 *            state and event handlers of the components created by the
	 *            returned factory.
	 * 
	 * @return the created factory.
	 */
	public Factory createFactory(String componentClassName)
			throws ClassNotFoundException;

	public Channel createChannel();

	public Channel getFaultChannel();

	// TODO document
	// TODO createFaultChannel
	public void subscribe(Channel channel, String eventHandlerName);

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
}
