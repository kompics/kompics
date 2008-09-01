package se.sics.kompics.p2p.network.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;

/**
 * The <code>PerfectNetNetworkDeliverEvent</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id: PerfectNetNetworkDeliverEvent.java 144 2008-06-04 15:27:08Z
 *          cosmin $
 */
@EventType
public final class PerfectNetNetworkDeliverEvent extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7947782637977816609L;

	private final Message perfectNetworkDeliverEvent;

	public PerfectNetNetworkDeliverEvent(Message perfectNetworkDeliverEvent,
			Address source, Address destination) {
		super(source, destination);
		this.perfectNetworkDeliverEvent = perfectNetworkDeliverEvent;
	}

	public final Message getPerfectNetworkDeliverEvent() {
		return perfectNetworkDeliverEvent;
	}
}
