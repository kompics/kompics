package se.sics.kompics.api;

public interface Component {

	public void triggerEvent(Event event);

	public void triggerEvent(Event event, Priority priority);

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

	// TODO document
	// TODO createFaultChannel
	public void subscribe(Channel channel, String eventHandlerName);

	public void bind(Class<? extends Event> eventType, Channel channel);

	public void start();

	public void stop();
}
