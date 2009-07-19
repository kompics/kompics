package se.sics.kompics.wan.plab.events;


import java.util.Map;

import se.sics.kompics.Request;

public class DnsResolverRequest extends Request {

	private final Map<Integer,String> hosts;
	public DnsResolverRequest(Map<Integer,String> hosts) {
		this.hosts = hosts;
	}
	
	public Map<Integer,String> getHosts() {
		return hosts;
	}
}
