package se.sics.kompics.api;

public interface Factory {

	public Component createComponent(Channel... channelParameters);
}
