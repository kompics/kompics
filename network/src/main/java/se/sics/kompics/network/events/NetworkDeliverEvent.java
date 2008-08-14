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
@SuppressWarnings("serial")
@EventType
public abstract class NetworkDeliverEvent implements Event, Serializable {

	private final Address source;

	public final Address destination;

	private transient Transport protocol;

	protected NetworkDeliverEvent(Address source, Address destination) {
		this.source = source;
		this.destination = destination;
	}

	public final Address getSource() {
		return source;
	}

	public final Address getDestination() {
		return destination;
	}

	public final Transport getProtocol() {
		return protocol;
	}

	public final void setProtocol(Transport protocol) {
		this.protocol = protocol;
	}
}
