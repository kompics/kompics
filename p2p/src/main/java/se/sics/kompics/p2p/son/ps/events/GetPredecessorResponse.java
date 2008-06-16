package se.sics.kompics.p2p.son.ps.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;

/**
 * The <code>GetPredecessorResponse</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: GetPredecessorResponse.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class GetPredecessorResponse extends PerfectNetworkDeliverEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1912385214693831546L;

	private final Address predecessor;

	public GetPredecessorResponse(Address predecessor) {
		this.predecessor = predecessor;
	}

	public Address getPredecessor() {
		return predecessor;
	}
}
