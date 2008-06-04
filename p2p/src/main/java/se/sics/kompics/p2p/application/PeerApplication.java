package se.sics.kompics.p2p.application;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;

/**
 * The <code>PeerApplication</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class PeerApplication {

	private final Component component;

	public PeerApplication(Component component) {
		this.component = component;
	}

	@ComponentCreateMethod
	public void create() {
		;
	}
}
