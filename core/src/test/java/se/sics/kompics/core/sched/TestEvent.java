package se.sics.kompics.core.sched;

import org.junit.Ignore;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

@Ignore
@EventType
public final class TestEvent implements Event {

	private final String message;

	public TestEvent() {
		super();
		this.message = null;
	}

	public TestEvent(String message) {
		super();
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
