package se.sics.kompics.p2p.son.ps.events;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;

/**
 * The <code>JoinRingCompleted</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: JoinRingCompleted.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class JoinRingCompleted implements Event {

	private final Address localPeer;

	public JoinRingCompleted(Address localPeer) {
		super();
		this.localPeer = localPeer;
	}

	public Address getLocalPeer() {
		return localPeer;
	}
}
