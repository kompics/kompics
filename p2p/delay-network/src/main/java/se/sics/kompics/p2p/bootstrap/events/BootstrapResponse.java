package se.sics.kompics.p2p.bootstrap.events;

import java.util.Set;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

/**
 * The <code>BootstrapResponse</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id: BootstrapResponse.java 142 2008-06-04 15:10:22Z cosmin $
 */
@EventType
public final class BootstrapResponse implements Event {

	private final Set<PeerEntry> peers;

	public BootstrapResponse(Set<PeerEntry> peers) {
		this.peers = peers;
	}

	public Set<PeerEntry> getPeers() {
		return peers;
	}
}
