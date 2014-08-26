package se.sics.kompics.network;

import java.net.InetSocketAddress;
import java.util.HashSet;

import se.sics.kompics.Response;

public class ConnectionStatusResponse extends Response {

	public final HashSet<InetSocketAddress> tcp;
	public final HashSet<InetSocketAddress> udp;

	public ConnectionStatusResponse(ConnectionStatusRequest request,
			HashSet<InetSocketAddress> tcp, HashSet<InetSocketAddress> udp) {
		super(request);
		this.tcp = tcp;
		this.udp = udp;
	}
}
