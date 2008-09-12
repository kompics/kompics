package se.sics.kompics.api;


public interface EventFilter<E extends Event> {

	public boolean filter(E event);
}
