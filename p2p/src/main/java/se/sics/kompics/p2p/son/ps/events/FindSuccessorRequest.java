package se.sics.kompics.p2p.son.ps.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
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
	private static final long serialVersionUID = -5529275130279265974L;

	private final Address peer;

	public FindSuccessorRequest(Address peer) {
		super();
		this.peer = peer;
	}

	public Address getPeer() {
		return peer;
	}
}
