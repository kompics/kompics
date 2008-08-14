package se.sics.kompics.api;

import java.util.Set;

public interface Channel {

	public boolean hasEventType(Class<? extends Event> eventType);

	public Set<Class<? extends Event>> getEventTypes();
}
