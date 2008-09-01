package se.sics.kompics.p2p.bootstrap.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;

/**
 * The <code>CacheGetPeersRequest</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public final class CacheGetPeersRequest extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2728539355659872087L;

	private final int peersMax;

	private final long requestId;

	public CacheGetPeersRequest(int peersMax, long id, Address destination) {
		super(destination);
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
