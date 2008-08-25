package se.sics.kompics.network.events;

import java.net.SocketAddress;

import se.sics.kompics.api.Event;

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
