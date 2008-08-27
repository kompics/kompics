package se.sics.kompics.p2p.chord.events;

import java.io.Serializable;
import java.util.List;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.p2p.chord.router.FingerTableView;

/**
 * The <code>GetChordNeighborsResponse</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: GetChordNeighborsResponse.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class GetChordNeighborsResponse implements Event, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2356239993994511579L;

	private final Address localPeer;

	private final Address predecessorPeer;

	private final Address successorPeer;

	private final List<Address> successorList;

	private final FingerTableView fingerTable;

	public GetChordNeighborsResponse(Address localPeer, Address successor,
			Address predecessor, List<Address> successorList,
			FingerTableView fingerTable) {
		super();
		this.localPeer = localPeer;
		this.predecessorPeer = predecessor;
		this.successorPeer = successor;
		this.successorList = successorList;
		this.fingerTable = fingerTable;
	}

	public Address getLocalPeer() {
		return localPeer;
	}

	public Address getSuccessorPeer() {
		return successorPeer;
	}

	public Address getPredecessorPeer() {
		return predecessorPeer;
	}

	public List<Address> getSuccessorList() {
		return successorList;
	}

	public FingerTableView getFingerTable() {
		return fingerTable;
	}
}
