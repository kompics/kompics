package se.sics.kompics.p2p.bootstrap.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;

/**
 * The <code>CacheGetPeersRequest</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public final class CacheGetPeersRequest extends PerfectNetworkDeliverEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4732457498826276158L;

	private final int peersMax;

	private final long requestId;

	public CacheGetPeersRequest(int peersMax, long id) {
		this.peersMax = peersMax;
		this.requestId = id;
	}

	public int getPeersMax() {
		return peersMax;
	}

	public long getRequestId() {
		return requestId;
	}
}
