package se.sics.kompics.p2p.chord.router.events;

import java.math.BigInteger;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;

/**
 * The <code>FindSuccessorRequest</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: FindSuccessorRequest.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class FindSuccessorRequest extends PerfectNetworkDeliverEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5718478114531620356L;

	private final BigInteger key;

	private final long lookupId;

	public FindSuccessorRequest(BigInteger key, long lookupId) {
		super();
		this.key = key;
		this.lookupId = lookupId;
	}

	public BigInteger getKey() {
		return key;
	}

	public long getLookupId() {
		return lookupId;
	}
}
