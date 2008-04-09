package se.sics.kompics.api;

public interface Channel {

	public void addEventType(Class<? extends Event> eventType);

	public void removeEventType(Class<? extends Event> eventType);

	public boolean hasEventType(Class<? extends Event> eventType);
}
