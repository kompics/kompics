package org.component.chord.peer;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

public final class ComponentMessage extends Message {

	private final int attribute;

	public ComponentMessage(Address source, Address destination, int attribute) {
		super(source, destination);
		this.attribute = attribute;
	}

	public int getAttribute() {
		return attribute;
	}
}
