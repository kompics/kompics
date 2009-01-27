package se.sics.kompics.p2p.bootstrap.events;

import java.util.Set;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;

/**
 * The <code>CacheGetPeersResponse</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public final class CacheGetPeersResponse extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -765722946138332345L;

	private final Set<PeerEntry> peers;

	private final long reqestId;

	public CacheGetPeersResponse(Set<PeerEntry> peers, long id,
			Address destination) {
		super(destination);
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
