package se.sics.kompics.network.events;

import java.net.SocketAddress;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

/**
 * The <code>NetworkSessionClosed</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id: NetworkSessionClosed.java 188 2008-08-14 20:40:48Z Cosmin $
 */
@EventType
public class NetworkSessionClosed implements Event {

	private final SocketAddress remoteAddress;

	public SocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	public NetworkSessionClosed(SocketAddress remoteAddress) {
		super();
		this.remoteAddress = remoteAddress;
	}
}
