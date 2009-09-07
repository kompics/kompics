package se.sics.kompics.wan.plab.events;


import java.net.InetAddress;
import java.util.Map;

import se.sics.kompics.Response;

public class DnsResolverResponse extends Response {

	private final Map<Integer,InetAddress> ipAddrs;
	
	public DnsResolverResponse(DnsResolverRequest request, Map<Integer,InetAddress> ipAddrs) {
		super(request);
		this.ipAddrs = ipAddrs;
	}
	
	public Map<Integer,InetAddress> getIpAddrs() {
		return ipAddrs;
	}
}
