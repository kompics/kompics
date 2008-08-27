package se.sics.kompics.p2p.fd.events;

import java.util.LinkedList;

import se.sics.kompics.api.Event;
import se.sics.kompics.network.Address;

/**
 * The <code>StatusResponse</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: StatusResponse.java 294 2006-05-05 17:14:14Z roberto $
 */
public final class StatusResponse implements Event {

	private final LinkedList<Address> probedPeers;

	public StatusResponse(LinkedList<Address> probedPeers) {
		super();
		this.probedPeers = probedPeers;
	}

	public LinkedList<Address> getProbedPeers() {
		return probedPeers;
	}
}
