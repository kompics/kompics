package se.sics.kompics.p2p.son.ps.events;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;

/**
 * The <code>NewSuccessor</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: NewSuccessor.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class NewSuccessor implements Event {

	private final Address localPeer;

	private final Address successorPeer;

	public NewSuccessor(Address localPeer, Address successorPeer) {
		super();
		this.localPeer = localPeer;
		this.successorPeer = successorPeer;
	}

	public Address getLocalPeer() {
		return localPeer;
	}

	public Address getSuccessorPeer() {
		return successorPeer;
	}
}
