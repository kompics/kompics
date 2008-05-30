package se.sics.kompics.p2p.son;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;

@ComponentSpecification
public class StructuredOverlay {

	private final Component component;

	public StructuredOverlay(Component component) {
		this.component = component;
	}

	@ComponentCreateMethod
	public void create() {
		;
	}
}
