package se.sics.kompics.example;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

@EventType
public class HelloEvent implements Event {
	private final String message;

	public HelloEvent(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
