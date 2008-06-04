package se.sics.kompics.p2p.network.events;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;

/**
 * The <code>PerfectNetworkSendEvent</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public final class PerfectNetworkSendEvent implements Event {

	private final Address destination;

	private final PerfectNetworkDeliverEvent perfectNetworkDeliverEvent;

	public PerfectNetworkSendEvent(
			PerfectNetworkDeliverEvent perfectNetworkDeliverEvent,
			Address destination) {
		this.destination = destination;
		this.perfectNetworkDeliverEvent = perfectNetworkDeliverEvent;
	}

	public final Address getDestination() {
		return destination;
	}

	public final PerfectNetworkDeliverEvent getPerfectNetworkDeliverEvent() {
		return perfectNetworkDeliverEvent;
	}
}
