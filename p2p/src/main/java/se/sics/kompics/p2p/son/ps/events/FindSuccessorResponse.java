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
	private static final long serialVersionUID = 5575533107716792158L;

	private final Address successor;

	public FindSuccessorResponse(Address successor) {
		this.successor = successor;
	}

	public Address getSuccessor() {
		return successor;
	}
}
