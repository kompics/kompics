package se.sics.kompics.p2p.chord.ring.events;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;

/**
 * The <code>NewPredecessor</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: NewPredecessor.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class NewPredecessor implements Event {

	private final Address localPeer;

	private final Address predecessorPeer;

	public NewPredecessor(Address localPeer, Address predecessorPeer) {
		super();
		this.localPeer = localPeer;
		this.predecessorPeer = predecessorPeer;
	}

	public Address getLocalPeer() {
		return localPeer;
	}

	public Address getPredecessorPeer() {
		return predecessorPeer;
	}
}
