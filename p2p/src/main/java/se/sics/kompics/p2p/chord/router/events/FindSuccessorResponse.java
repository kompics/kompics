package se.sics.kompics.p2p.chord.router.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;

/**
 * The <code>FindSuccessorResponse</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: FindSuccessorResponse.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class FindSuccessorResponse extends PerfectNetworkDeliverEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7818952658643612459L;

	private final Address successor;

	private final boolean nextHop;

	private final long lookupId;

	public FindSuccessorResponse(Address successor, long lookupId,
			boolean nextHop) {
		this.successor = successor;
		this.nextHop = nextHop;
		this.lookupId = lookupId;
	}

	public Address getSuccessor() {
		return successor;
	}

	public long getLookupId() {
		return lookupId;
	}

	public boolean isNextHop() {
		return nextHop;
	}
}
