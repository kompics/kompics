package se.sics.kompics.p2p.peer;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;

/**
 * The <code>Peer</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class Peer {

	private final Component component;

	public Peer(Component component) {
		this.component = component;
	}

	@ComponentCreateMethod
	public void create() {
		;
	}
}
