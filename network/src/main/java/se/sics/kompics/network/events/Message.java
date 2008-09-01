package se.sics.kompics.network.events;

import java.io.Serializable;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Transport;

/**
 * The <code>Message</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@SuppressWarnings("serial")
@EventType
public abstract class Message implements Event, Serializable {

	private Address source;

	public final Address destination;

	private transient Transport protocol;

	protected Message(Address destination) {
		this.destination = destination;
		this.protocol = Transport.TCP;
	}

	protected Message(Address source, Address destination) {
		this.source = source;
		this.destination = destination;
		this.protocol = Transport.TCP;
	}

	protected Message(Address destination, Transport protocol) {
		this.destination = destination;
		this.protocol = protocol;
	}

	protected Message(Address source, Address destination, Transport protocol) {
		this.source = source;
		this.destination = destination;
		this.protocol = protocol;
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

	public final void setSource(Address address) {
		this.source = address;
	}
}
