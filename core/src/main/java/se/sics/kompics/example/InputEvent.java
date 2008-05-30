package se.sics.kompics.example;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

@EventType
public class InputEvent implements Event {

	private final int attribute1;

	private final String attribute2;

	public InputEvent(int attribute1, String attribute2) {
		super();
		this.attribute1 = attribute1;
		this.attribute2 = attribute2;
	}

	public int attribute1() {
		return attribute1;
	}

	public String attribute2() {
		return attribute2;
	}
}
