package se.sics.kompics.p2p.peer.events;

import java.math.BigInteger;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

/**
 * The <code>JoinPeer</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id: JoinPeer.java 142 2008-06-04 15:10:22Z cosmin $
 */
@EventType
public final class JoinPeer implements Event {

	private final BigInteger peerId;

	public JoinPeer(BigInteger peerId) {
		super();
		this.peerId = peerId;
	}

	public BigInteger getPeerId() {
		return peerId;
	}
}
