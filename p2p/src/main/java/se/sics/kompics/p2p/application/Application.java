package se.sics.kompics.p2p.application;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;

/**
 * The <code>Application</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class Application {

	private final Component component;

	public Application(Component component) {
		this.component = component;
	}

	@ComponentCreateMethod
	public void create() {
		;
	}
}
