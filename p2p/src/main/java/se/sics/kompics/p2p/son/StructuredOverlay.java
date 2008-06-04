package se.sics.kompics.p2p.son;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;

/**
 * The <code>StructuredOverlay</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
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
