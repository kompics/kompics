package se.sics.kompics.api;


public interface GuardedEventHandler<E extends Event> extends EventHandler<E> {

	public boolean guard(E event);
}
