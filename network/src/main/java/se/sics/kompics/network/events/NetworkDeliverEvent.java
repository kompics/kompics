package se.sics.kompics.network.events;

import java.io.Serializable;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Transport;

/**
 * The <code>NetworkDeliverEvent</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public abstract class NetworkDeliverEvent implements Event, Serializable {

	private transient Address source;

	private transient Address destination;

	private transient Transport protocol;

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

	public final Transport getProtocol() {
		return protocol;
	}

	public final void setProtocol(Transport protocol) {
		this.protocol = protocol;
	}
}
