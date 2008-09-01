package se.sics.kompics.p2p.bootstrap.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;

/**
 * The <code>CacheResetRequest</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public final class CacheResetRequest extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4122752651550459016L;

	private final Address peerAddress;

	public CacheResetRequest(Address peerAddress, Address destination) {
		super(destination);
		this.peerAddress = peerAddress;
	}

	public Address getPeerAddress() {
		return peerAddress;
	}
}
