package se.sics.kompics.p2p.son.ps;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;

@ComponentSpecification
public class ChordPeriodicStabilization {

	private final Component component;

	public ChordPeriodicStabilization(Component component) {
		this.component = component;
	}

	@ComponentCreateMethod
	public void create() {
		;
	}
}
