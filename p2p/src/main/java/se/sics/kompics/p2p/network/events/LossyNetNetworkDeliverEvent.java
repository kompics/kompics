package se.sics.kompics.p2p.network.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.NetworkDeliverEvent;

/**
 * The <code>LossyNetNetworkDeliverEvent</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public final class LossyNetNetworkDeliverEvent extends NetworkDeliverEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7947782637977816609L;

	private final LossyNetworkDeliverEvent flp2pDeliverEvent;

	public LossyNetNetworkDeliverEvent(
			LossyNetworkDeliverEvent flp2pDeliverEvent, Address source,
			Address destination) {
		super(source, destination);
		this.flp2pDeliverEvent = flp2pDeliverEvent;
	}

	public final LossyNetworkDeliverEvent getFlp2pDeliverEvent() {
		return flp2pDeliverEvent;
	}
}
