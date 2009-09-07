package se.sics.kompics.wan.hosts.events;

import java.util.HashSet;
import java.util.Set;

import se.sics.kompics.Response;
import se.sics.kompics.wan.ssh.Host;

public class GetNodesResponse extends Response {

	private final Set<Host> hosts;

	public GetNodesResponse(GetNodesRequest request, Set<Host> hosts) {
		super(request);
		this.hosts = new HashSet<Host>();
		for (Host h : hosts) {
			this.hosts.add(h);
		}
	}
	/**
	 * @return the nodeIds
	 */
	public Set<Host> getHosts() {
		return hosts;
	}
}
