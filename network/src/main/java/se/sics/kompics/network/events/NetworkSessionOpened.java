package se.sics.kompics.network.events;

import java.net.SocketAddress;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

/**
 * The <code>NetworkSessionOpened</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id: NetworkSessionOpened.java 188 2008-08-14 20:40:48Z Cosmin $
 */
@EventType
public class NetworkSessionOpened implements Event {

	private final SocketAddress remoteAddress;

	public SocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	public NetworkSessionOpened(SocketAddress remoteAddress) {
		super();
		this.remoteAddress = remoteAddress;
	}
}
