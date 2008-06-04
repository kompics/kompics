package se.sics.kompics.p2p.monitor;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;

/**
 * The <code>PeerMonitorClient</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class PeerMonitorClient {

	private final Component component;

	public PeerMonitorClient(Component component) {
		super();
		this.component = component;
	}

	@ComponentCreateMethod
	public void create() {
		;
	}
}
