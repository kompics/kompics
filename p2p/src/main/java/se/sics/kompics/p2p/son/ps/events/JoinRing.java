package se.sics.kompics.p2p.son.ps.events;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;

/**
 * The <code>JoinRing</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: JoinRing.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class JoinRing implements Event {

	private final Address insidePeer;

	public JoinRing(Address insidePeer) {
		super();
		this.insidePeer = insidePeer;
	}

	public Address getInsidePeer() {
		return insidePeer;
	}
}
