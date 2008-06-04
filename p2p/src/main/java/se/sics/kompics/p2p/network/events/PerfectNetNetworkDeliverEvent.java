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

	private final PerfectNetworkDeliverEvent pp2pDeliverEvent;

	public PerfectNetNetworkDeliverEvent(
			PerfectNetworkDeliverEvent pp2pDeliverEvent, Address source,
			Address destination) {
		super(source, destination);
		this.pp2pDeliverEvent = pp2pDeliverEvent;
	}

	public final PerfectNetworkDeliverEvent getPp2pDeliverEvent() {
		return pp2pDeliverEvent;
	}
}
