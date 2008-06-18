package se.sics.kompics.p2p.son.ps.events;

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
	private static final long serialVersionUID = -1891090539812366525L;

	private final Address successor;

	private final boolean nextHop;

	public FindSuccessorResponse(Address successor, boolean nextHop) {
		this.successor = successor;
		this.nextHop = nextHop;
	}

	public Address getSuccessor() {
		return successor;
	}

	public boolean isNextHop() {
		return nextHop;
	}
}
