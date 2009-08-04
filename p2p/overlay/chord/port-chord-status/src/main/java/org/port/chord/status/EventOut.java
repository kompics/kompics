package org.port.chord.status;

import se.sics.kompics.Event;

public final class EventOut extends Event {

	private final int attribute;

	public EventOut(int attribute) {
		this.attribute = attribute;
	}
	
	public int getAttribute() {
		return attribute;
	}
}
