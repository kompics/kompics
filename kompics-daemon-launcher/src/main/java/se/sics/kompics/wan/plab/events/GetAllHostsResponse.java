package se.sics.kompics.wan.plab.events;


import se.sics.kompics.Response;
import se.sics.kompics.wan.master.plab.plc.PlanetLabHost;

public class GetAllHostsResponse extends Response {

	private final PlanetLabHost[] hosts;
	
	public GetAllHostsResponse(GetAllHostsRequest request, PlanetLabHost[] hosts) {
		super(request);
		this.hosts = hosts;
	}
	
	public PlanetLabHost[] getHosts() {
		return hosts;
	}
}
