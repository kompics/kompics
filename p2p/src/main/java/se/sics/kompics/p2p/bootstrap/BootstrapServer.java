package se.sics.kompics.p2p.bootstrap;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;

/**
 * The <code>BootstrapServer</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class BootstrapServer {

	private final Component component;

	public BootstrapServer(Component component) {
		super();
		this.component = component;
	}

	@ComponentCreateMethod
	public void create() {
		;
	}
}
