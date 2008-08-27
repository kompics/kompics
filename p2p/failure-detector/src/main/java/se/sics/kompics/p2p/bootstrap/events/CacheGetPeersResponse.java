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
	private static final long serialVersionUID = -4244511822228360931L;

	private final Set<PeerEntry> peers;

	private final long reqestId;

	public CacheGetPeersResponse(Set<PeerEntry> peers, long id) {
		this.peers = peers;
		this.reqestId = id;
	}

	public Set<PeerEntry> getPeers() {
		return peers;
	}

	public long getReqestId() {
		return reqestId;
	}
}
