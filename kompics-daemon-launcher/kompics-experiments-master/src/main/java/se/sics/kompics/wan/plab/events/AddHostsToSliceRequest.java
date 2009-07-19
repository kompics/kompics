package se.sics.kompics.wan.plab.events;


import java.util.Set;

import se.sics.kompics.Request;

public class AddHostsToSliceRequest extends Request {

	private final Set<String> hosts;
	

	public AddHostsToSliceRequest(Set<String> hosts) {
		this.hosts = hosts;
	}
	
	public Set<String> getHosts() {
		return hosts;
	}
	
}
