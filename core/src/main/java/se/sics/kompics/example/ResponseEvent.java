package se.sics.kompics.example;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

@EventType
public class ResponseEvent implements Event {
	private final String message;

	public ResponseEvent(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
