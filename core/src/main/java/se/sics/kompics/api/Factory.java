package se.sics.kompics.api;

public interface Factory {

	/**
	 * Sets the parameters used to initialize the created component instances.
	 * 
	 * @param objects
	 *            the parameters
	 */
	public void setInitializationParameters(Object... objects);

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
	public Component createComponent(Channel faultChannel,
			Channel... channelParameters);
}
