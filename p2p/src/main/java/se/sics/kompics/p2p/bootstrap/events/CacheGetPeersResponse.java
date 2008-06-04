package se.sics.kompics.p2p.bootstrap.events;

import java.util.Set;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;

/**
 * The <code>CacheGetPeersResponse</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public final class CacheGetPeersResponse extends PerfectNetworkDeliverEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5849399496812824557L;

	private final Set<PeerEntry> peers;

	public CacheGetPeersResponse(Set<PeerEntry> peers) {
		this.peers = peers;
	}

	public Set<PeerEntry> getPeers() {
		return peers;
	}
}
