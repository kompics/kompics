package se.sics.kompics.p2p.chord.ring.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.p2p.chord.ring.RequestState;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;

/**
 * The <code>GetPredecessorRequest.java</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: GetPredecessorRequest.java.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class GetSuccessorListRequest extends PerfectNetworkDeliverEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7538518491687088133L;

	private final RequestState requestState;

	public GetSuccessorListRequest(RequestState requestState) {
		this.requestState = requestState;
	}

	public RequestState getRequestState() {
		return requestState;
	}
}
