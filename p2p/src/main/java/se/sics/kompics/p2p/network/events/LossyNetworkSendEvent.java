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

	private final LossyNetworkDeliverEvent lossyNetworkDeliverEvent;

	public LossyNetworkSendEvent(
			LossyNetworkDeliverEvent lossyNetworkDeliverEvent,
			Address destination) {
		this.destination = destination;
		this.lossyNetworkDeliverEvent = lossyNetworkDeliverEvent;
	}

	public final Address getDestination() {
		return destination;
	}

	public final LossyNetworkDeliverEvent getLossyNetworkDeliverEvent() {
		return lossyNetworkDeliverEvent;
	}
}
