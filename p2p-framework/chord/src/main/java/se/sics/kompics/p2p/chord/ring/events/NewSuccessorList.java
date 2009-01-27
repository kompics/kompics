package se.sics.kompics.p2p.chord.ring.events;

import java.util.ArrayList;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;

/**
 * The <code>NewSuccessorList</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: NewSuccessorList.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class NewSuccessorList implements Event {

	private final Address localPeer;

	private final ArrayList<Address> successorListView;

	public NewSuccessorList(Address localPeer,
			ArrayList<Address> successorListView) {
		super();
		this.localPeer = localPeer;
		this.successorListView = successorListView;
	}

	public Address getLocalPeer() {
		return localPeer;
	}

	public ArrayList<Address> getSuccessorListView() {
		return successorListView;
	}
}
