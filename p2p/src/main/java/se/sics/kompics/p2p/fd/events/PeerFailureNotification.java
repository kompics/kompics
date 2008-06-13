package se.sics.kompics.p2p.fd.events;

import se.sics.kompics.api.Event;
import se.sics.kompics.network.Address;

/**
 * The <code>PeerFailureNotification</code> class
 * 
 * @author Cosmin Arad
 * @author Roberto Roverso
 * @version $Id: PeerFailureNotification.java 139 2008-06-04 10:55:59Z cosmin $
 */
public final class PeerFailureNotification implements Event {

	private final Address peerAddress;

	private final PeerFailureStatus failureStatus;

	public PeerFailureNotification(Address peerAddress,
			PeerFailureStatus failureStatus) {
		super();
		this.peerAddress = peerAddress;
		this.failureStatus = failureStatus;
	}

	public Address getPeerAddress() {
		return peerAddress;
	}

	public PeerFailureStatus getPeerFailureStatus() {
		return failureStatus;
	}
}
