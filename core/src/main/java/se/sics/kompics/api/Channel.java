package se.sics.kompics.api;

import java.util.Set;

public interface Channel {

	public void addEventType(Class<? extends Event> eventType);

	public void removeEventType(Class<? extends Event> eventType);

	public boolean hasEventType(Class<? extends Event> eventType);

	public Set<Class<? extends Event>> getEventTypes();
}
