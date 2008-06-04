package se.sics.kompics.p2p.monitor;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;

/**
 * The <code>PeerMonitorServer</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class PeerMonitorServer {

	private final Component component;

	public PeerMonitorServer(Component component) {
		super();
		this.component = component;
	}

	@ComponentCreateMethod
	public void create() {
		;
	}
}
