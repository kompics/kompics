package se.sics.kompics.network;

import java.io.Serializable;

import se.sics.kompics.Event;
import se.sics.kompics.address.Address;

public abstract class Message extends Event implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2644373757327105586L;

	private final Address source;

	private final Address destination;
	
	private transient Transport protocol;

	protected Message(Address source, Address destination) {
		this(source, destination, Transport.TCP);
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
	
	public final void setProtocol(Transport protocol) {
		this.protocol = protocol;
	}
	
	public final Transport getProtocol() {
		return protocol;
	}
}
