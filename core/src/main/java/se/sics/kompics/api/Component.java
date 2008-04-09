package se.sics.kompics.api;

import se.sics.kompics.core.ChannelCore;

public interface Component {

	public void triggerEvent(Event event);

	public void triggerEvent(Event event, Priority priority);

	public void triggerEvent(Event event, ChannelCore channel);

	public void triggerEvent(Event event, ChannelCore channel, Priority priority);

}
