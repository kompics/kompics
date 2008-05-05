package se.sics.kompics.network.events;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Transport;

/**
 * The <code>NetworkSendEvent</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public class NetworkSendEvent implements Event {

	private Address source;

	private Address destination;

	private Transport protocol;

	private NetworkDeliverEvent networkDeliverEvent;

	public NetworkSendEvent(NetworkDeliverEvent networkDeliverEvent,
			Address source, Address destination, Transport protocol) {
		if (networkDeliverEvent == null)
			throw new RuntimeException(
					"I shall not send a null NetworkDeliverEvent");
		if (source == null || destination == null)
			throw new RuntimeException(
					"Source and destination addresses cannot be null");
		if (protocol == null) {
			this.protocol = Transport.UDP;
		} else {
			this.protocol = protocol;
		}

		this.networkDeliverEvent = networkDeliverEvent;
		this.destination = destination;
		this.source = source;
	}

	public NetworkSendEvent(NetworkDeliverEvent networkDeliverEvent,
			Address source, Address destination) {
		this(networkDeliverEvent, source, destination, Transport.UDP);
	}

	public NetworkSendEvent(NetworkDeliverEvent networkDeliverEvent,
			Address destination) {
		this(networkDeliverEvent, Address.getLocalAddress(), destination,
				Transport.UDP);
	}

	public NetworkSendEvent(NetworkDeliverEvent networkDeliverEvent,
			Address destination, Transport protocol) {
		this(networkDeliverEvent, Address.getLocalAddress(), destination,
				protocol);
	}

	public Address getDestination() {
		return destination;
	}

	public Address getSource() {
		return source;
	}

	public Transport getProtocol() {
		return protocol;
	}

	public NetworkDeliverEvent getNetworkDeliverEvent() {
		return networkDeliverEvent;
	}
}
