package se.sics.kompics.wan.plab.events;

import java.util.ArrayList;
import java.util.List;

import se.sics.kompics.Response;
import se.sics.kompics.wan.plab.PLabHost;

public class GetNodesResponse extends Response {

	private final List<PLabHost> hosts;

	public GetNodesResponse(GetNodesRequest request,
			List<PLabHost> hosts) {
		super(request);
		this.hosts = new ArrayList<PLabHost>(hosts);
	}
	
	/**
	 * @return the hosts
	 */
	public List<PLabHost> getHosts() {
		return hosts;
	}

}
