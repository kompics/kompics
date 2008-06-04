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
	private static final long serialVersionUID = 8012974467319313378L;

	private final int peersMax;

	public CacheGetPeersRequest(int peersMax) {
		this.peersMax = peersMax;
	}

	public int getPeersMax() {
		return peersMax;
	}
}
