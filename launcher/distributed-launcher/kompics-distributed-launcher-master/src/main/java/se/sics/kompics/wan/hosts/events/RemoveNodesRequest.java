package se.sics.kompics.wan.hosts.events;


import java.util.Set;

import se.sics.kompics.Request;
import se.sics.kompics.wan.ssh.Host;

public class RemoveNodesRequest extends Request {

	private final Set<Host> hosts;
	

	public RemoveNodesRequest(Set<Host> hosts) {
		this.hosts = hosts;
	}
	
	public Set<Host> getHosts() {
		return hosts;
	}
	
}
