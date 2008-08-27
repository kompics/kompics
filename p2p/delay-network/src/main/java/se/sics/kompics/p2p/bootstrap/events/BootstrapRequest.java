package se.sics.kompics.p2p.bootstrap.events;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

/**
 * The <code>BootstrapRequest</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id: BootstrapRequest.java 142 2008-06-04 15:10:22Z cosmin $
 */
@EventType
public final class BootstrapRequest implements Event {

	private final int peersMax;

	public BootstrapRequest(int peersMax) {
		this.peersMax = peersMax;
	}

	public int getPeersMax() {
		return peersMax;
	}
}
