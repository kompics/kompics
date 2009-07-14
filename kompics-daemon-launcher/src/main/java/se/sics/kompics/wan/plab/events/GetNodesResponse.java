package se.sics.kompics.wan.plab.events;

import java.util.HashSet;
import java.util.Set;

import se.sics.kompics.Response;
import se.sics.kompics.wan.plab.PLabHost;

public class GetNodesResponse extends Response {

	private final Set<PLabHost> hosts;

	public GetNodesResponse(GetNodesRequest request,
			Set<PLabHost> hosts) {
		super(request);
		this.hosts = new HashSet<PLabHost>(hosts);
	}
	
	/**
	 * @return the hosts
	 */
	public Set<PLabHost> getHosts() {
		return hosts;
	}

}
