package org.port.cyclon.status;

import se.sics.kompics.Event;

public final class EventIn extends Event {

	private final int attribute;

	public EventIn(int attribute) {
		this.attribute = attribute;
	}
	
	public int getAttribute() {
		return attribute;
	}
}
