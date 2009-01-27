package se.sics.kompics.p2p.fd.events;

import se.sics.kompics.api.Event;
import se.sics.kompics.network.Address;

/**
 * The <code>PeerFailureSuspicion</code> class
 * 
 * @author Cosmin Arad
 * @author Roberto Roverso
 * @version $Id: PeerFailureSuspicion.java 139 2008-06-04 10:55:59Z cosmin $
 */
public final class PeerFailureSuspicion implements Event {

	private final Address peerAddress;

	private final SuspicionStatus suspicionStatus;

	public PeerFailureSuspicion(Address peerAddress,
			SuspicionStatus suspicionStatus) {
		super();
		this.peerAddress = peerAddress;
		this.suspicionStatus = suspicionStatus;
	}

	public Address getPeerAddress() {
		return peerAddress;
	}

	public SuspicionStatus getSuspicionStatus() {
		return suspicionStatus;
	}
}
