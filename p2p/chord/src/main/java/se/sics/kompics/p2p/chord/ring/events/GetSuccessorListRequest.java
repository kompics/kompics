package se.sics.kompics.p2p.chord.ring.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;
import se.sics.kompics.p2p.chord.ring.RequestState;

/**
 * The <code>GetPredecessorRequest.java</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: GetPredecessorRequest.java.java 158 2008-06-16 10:42:01Z Cosmin
 *          $
 */
@EventType
public final class GetSuccessorListRequest extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8525044104672536587L;

	private final RequestState requestState;

	public GetSuccessorListRequest(RequestState requestState,
			Address destination) {
		super(destination);
		this.requestState = requestState;
	}

	public RequestState getRequestState() {
		return requestState;
	}
}
