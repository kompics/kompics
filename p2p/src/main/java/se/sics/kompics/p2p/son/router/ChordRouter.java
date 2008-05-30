package se.sics.kompics.p2p.son.router;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;

@ComponentSpecification
public class ChordRouter {

	private final Component component;

	public ChordRouter(Component component) {
		this.component = component;
	}

	@ComponentCreateMethod
	public void create() {
		;
	}
}