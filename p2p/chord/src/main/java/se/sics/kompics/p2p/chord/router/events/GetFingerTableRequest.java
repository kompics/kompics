package se.sics.kompics.p2p.chord.router.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;

/**
 * The <code>GetFingerTableRequest</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: GetFingerTableRequest.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class GetFingerTableRequest extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4859463233069782946L;

	public GetFingerTableRequest(Address destination) {
		super(destination);
	}
}
