package se.sics.kompics.p2p.fd.events;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.Event;
import se.sics.kompics.network.Address;

/**
 * The <code>StopProbingPeer</code> class
 * 
 * @author Cosmin Arad
 * @author Roberto Roverso
 * @version $Id: StopProbingPeer.java 294 2006-05-05 17:14:14Z roberto $
 */
public final class StopProbingPeer implements Event {

	private final Address peerAddress;

	private final Component component;

	public StopProbingPeer(Address peerAddress, Component component) {
		this.peerAddress = peerAddress;
		this.component = component;
	}

	public Address getPeerAddress() {
		return peerAddress;
	}

	public Component getComponent() {
		return component;
	}
}
