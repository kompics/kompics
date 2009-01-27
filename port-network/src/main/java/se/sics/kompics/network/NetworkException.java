package se.sics.kompics.network;

import java.net.SocketAddress;

import se.sics.kompics.Event;

public final class NetworkException extends Event {

	private final SocketAddress remoteAddress;

	public NetworkException(SocketAddress remoteAddress) {
		super();
		this.remoteAddress = remoteAddress;
	}

	public final SocketAddress getRemoteAddress() {
		return remoteAddress;
	}
}
