package se.sics.kompics.wan.master.plab.plc.events;

import se.sics.kompics.Response;
import se.sics.kompics.wan.master.plab.PLabHost;

public class GetNodesResponse extends Response {

	private final PLabHost[] hosts;

	public GetNodesResponse(GetNodesRequest request,
			PLabHost[] hosts) {
		super(request);
		this.hosts = hosts;
	}
	
	/**
	 * @return the hosts
	 */
	public PLabHost[] getHosts() {
		return hosts;
	}

}
