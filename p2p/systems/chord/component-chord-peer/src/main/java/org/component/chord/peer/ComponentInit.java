package org.component.chord.peer;

import se.sics.kompics.Init;

public final class ComponentInit extends Init {

	private final int attribute;

	public ComponentInit(int attribute) {
		this.attribute = attribute;
	}
	
	public int getAttribute() {
		return attribute;
	}
}
