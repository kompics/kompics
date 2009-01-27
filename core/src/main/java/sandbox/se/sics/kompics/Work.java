package sandbox.se.sics.kompics;

public final class Work {

	private final Event event;

	private final Handler<? extends Event> handler;

	public Work(Event event, Handler<? extends Event> handler) {
		this.event = event;
		this.handler = handler;
	}

	public final Event getEvent() {
		return event;
	}

	public final Handler<? extends Event> getHandler() {
		return handler;
	}
}
