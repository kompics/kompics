package se.sics.kompics.p2p.bootstrap;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;

/**
 * The <code>BootstrapClient</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class BootstrapClient {

	private final Component component;

	public BootstrapClient(Component component) {
		super();
		this.component = component;
	}

	@ComponentCreateMethod
	public void create() {
		;
	}
}
