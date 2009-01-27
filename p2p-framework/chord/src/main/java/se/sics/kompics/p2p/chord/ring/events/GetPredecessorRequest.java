package se.sics.kompics.p2p.chord.ring.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;

/**
 * The <code>GetPredecessorRequest</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: GetPredecessorRequest.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class GetPredecessorRequest extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7340813785668991486L;

	public GetPredecessorRequest(Address source, Address destination) {
		super(source, destination);
	}
}
