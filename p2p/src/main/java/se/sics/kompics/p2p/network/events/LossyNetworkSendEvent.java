package se.sics.kompics.p2p.network.events;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;

/**
 * The <code>LossyNetworkSendEvent</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public final class LossyNetworkSendEvent implements Event {

	private final Address destination;

	private final LossyNetworkDeliverEvent flp2pDeliverEvent;

	public LossyNetworkSendEvent(LossyNetworkDeliverEvent flp2pDeliverEvent,
			Address destination) {
		this.destination = destination;
		this.flp2pDeliverEvent = flp2pDeliverEvent;
	}

	public final Address getDestination() {
		return destination;
	}

	public final LossyNetworkDeliverEvent getFlp2pDeliverEvent() {
		return flp2pDeliverEvent;
	}
}
