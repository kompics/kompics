package se.sics.kompics.p2p.network.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.NetworkDeliverEvent;

/**
 * The <code>PerfectNetNetworkDeliverEvent</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public final class PerfectNetNetworkDeliverEvent extends NetworkDeliverEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7947782637977816609L;

	private final PerfectNetworkDeliverEvent perfectNetworkDeliverEvent;

	public PerfectNetNetworkDeliverEvent(
			PerfectNetworkDeliverEvent perfectNetworkDeliverEvent,
			Address source, Address destination) {
		super(source, destination);
		this.perfectNetworkDeliverEvent = perfectNetworkDeliverEvent;
	}

	public final PerfectNetworkDeliverEvent getPerfectNetworkDeliverEvent() {
		return perfectNetworkDeliverEvent;
	}
}
