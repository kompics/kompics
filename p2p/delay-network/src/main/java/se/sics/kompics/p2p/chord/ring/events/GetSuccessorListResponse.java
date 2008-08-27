package se.sics.kompics.p2p.chord.ring.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.p2p.chord.ring.RequestState;
import se.sics.kompics.p2p.chord.ring.SuccessorList;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;

/**
 * The <code>GetSuccessorListResponse</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: GetSuccessorListResponse.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class GetSuccessorListResponse extends PerfectNetworkDeliverEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2939794195234444500L;

	private final SuccessorList successorList;

	private final RequestState requestState;

	public GetSuccessorListResponse(SuccessorList successorList,
			RequestState requestState) {
		this.successorList = successorList;
		this.requestState = requestState;
	}

	public SuccessorList getSuccessorList() {
		return successorList;
	}

	public RequestState getRequestState() {
		return requestState;
	}
}
