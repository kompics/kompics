package se.sics.kompics.example;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

@EventType
public class InputEvent implements Event {

	private final int attribute;

	public InputEvent(int attribute) {
		super();
		this.attribute = attribute;
	}

	public int attribute() {
		return attribute;
	}
}
