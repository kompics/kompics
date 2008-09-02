package se.sics.kompics.p2p.bootstrap.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.timer.events.Timeout;

/**
 * The <code>ClientRefreshPeer</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id: ClientRefreshPeer.java 142 2008-06-04 15:10:22Z cosmin $
 */
@EventType
public final class ClientRefreshPeer extends Timeout {

	private final Address peerAddress;

	public ClientRefreshPeer(Address peerAddress) {
		this.peerAddress = peerAddress;
	}

	public Address getPeerAddress() {
		return peerAddress;
	}
}
