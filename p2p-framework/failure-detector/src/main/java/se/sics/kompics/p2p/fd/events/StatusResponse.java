package se.sics.kompics.p2p.fd.events;

import java.util.Map;

import se.sics.kompics.api.Event;
import se.sics.kompics.network.Address;
import se.sics.kompics.p2p.fd.ProbedPeerData;

/**
 * The <code>StatusResponse</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: StatusResponse.java 294 2006-05-05 17:14:14Z roberto $
 */
public final class StatusResponse implements Event {

	private final Map<Address, ProbedPeerData> probedPeers;

	public StatusResponse(Map<Address, ProbedPeerData> probedPeers) {
		super();
		this.probedPeers = probedPeers;
	}

	public Map<Address, ProbedPeerData> getProbedPeers() {
		return probedPeers;
	}
}
