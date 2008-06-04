package se.sics.kompics.p2p.web;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;

/**
 * The <code>WebHandler</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class WebHandler {

	private final Component component;

	public WebHandler(Component component) {
		super();
		this.component = component;
	}

	@ComponentCreateMethod
	public void create() {
		;
	}
}
