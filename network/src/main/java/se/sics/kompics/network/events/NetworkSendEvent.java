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
public final class NetworkSendEvent implements Event {

	private final Address source;

	private final Address destination;

	private final Transport protocol;

	private final NetworkDeliverEvent networkDeliverEvent;

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

	public final Address getDestination() {
		return destination;
	}

	public final Address getSource() {
		return source;
	}

	public final Transport getProtocol() {
		return protocol;
	}

	public final NetworkDeliverEvent getNetworkDeliverEvent() {
		return networkDeliverEvent;
	}
}
