package se.sics.kompics.p2p.network.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;

/**
 * The <code>LossyNetNetworkDeliverEvent</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id: LossyNetNetworkDeliverEvent.java 144 2008-06-04 15:27:08Z
 *          cosmin $
 */
@EventType
public final class LossyNetNetworkDeliverEvent extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7947782637977816609L;

	private final LossyNetworkDeliverEvent lossyNetworkDeliverEvent;

	public LossyNetNetworkDeliverEvent(
			LossyNetworkDeliverEvent lossyNetworkDeliverEvent, Address source,
			Address destination) {
		super(source, destination);
		this.lossyNetworkDeliverEvent = lossyNetworkDeliverEvent;
	}

	public final LossyNetworkDeliverEvent getLossyNetworkDeliverEvent() {
		return lossyNetworkDeliverEvent;
	}
}
