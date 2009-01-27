package se.sics.kompics;

public abstract class Filter<E extends Event> {

	Port <? extends PortType> port;
	
	Filter<?> next;
	
	public Filter() {
		port = null;
		next = null;
	}
	
	public Filter<E> on(Port<? extends PortType> port) {
		this.port = port;
		return this;
	}

	public Filter<?> or(Filter<?> filter) {
		this.next = filter;
		return filter;
	}

	protected abstract boolean filter(E event);
}
