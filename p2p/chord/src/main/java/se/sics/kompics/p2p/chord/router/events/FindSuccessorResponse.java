package se.sics.kompics.p2p.chord.router.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;

/**
 * The <code>FindSuccessorResponse</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: FindSuccessorResponse.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class FindSuccessorResponse extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4046121554567584478L;

	private final Address successor;

	private final boolean nextHop;

	private final long lookupId;

	public FindSuccessorResponse(Address successor, long lookupId,
			boolean nextHop, Address destination) {
		super(destination);
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
