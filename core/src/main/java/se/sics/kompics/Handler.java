package se.sics.kompics;

public abstract class Handler<E extends Event> {

	Class<E> eventType = null;

	public Handler() {
	}

	public Handler(Class<E> eventType) {
		this.eventType = eventType;
	}

	public abstract void handle(E event);

	public void setEventType(Class<E> eventType) {
		this.eventType = eventType;
	}

	public Class<E> getEventType() {
		return eventType;
	}
}
