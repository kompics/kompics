package se.sics.kompics.api;

public interface EventHandler<E extends Event> {

	public void handle(E event);
}
