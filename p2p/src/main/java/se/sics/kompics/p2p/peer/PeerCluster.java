package se.sics.kompics.p2p.peer;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.FaultEvent;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.EventHandlerMethod;

/**
 * The <code>PeerCluster</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class PeerCluster {

	private final Component component;

	public PeerCluster(Component component) {
		this.component = component;
	}

	@ComponentCreateMethod
	public void create() {
		;
	}

	@ComponentInitializeMethod
	public void init() {
		;
	}

	@EventHandlerMethod
	public void handleFaultEvent(FaultEvent event) {

	}
}
