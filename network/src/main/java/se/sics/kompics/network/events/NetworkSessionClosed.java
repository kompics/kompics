package se.sics.kompics.network.events;

import java.net.SocketAddress;

import se.sics.kompics.api.Event;

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
