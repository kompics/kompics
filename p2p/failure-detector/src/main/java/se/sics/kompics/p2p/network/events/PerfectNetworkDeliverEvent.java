package se.sics.kompics.p2p.network.events;

import java.io.Serializable;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;

/**
 * The <code>PerfectNetworkDeliverEvent</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id: PerfectNetworkDeliverEvent.java 139 2008-06-04 10:55:59Z cosmin
 *          $
 */
@SuppressWarnings("serial")
@EventType
public abstract class PerfectNetworkDeliverEvent implements Event, Serializable {

	private transient Address source;

	private transient Address destination;

	// TODO constructor

	public final Address getSource() {
		return source;
	}

	public final void setSource(Address source) {
		this.source = source;
	}

	public final Address getDestination() {
		return destination;
	}

	public final void setDestination(Address destination) {
		this.destination = destination;
	}
}
