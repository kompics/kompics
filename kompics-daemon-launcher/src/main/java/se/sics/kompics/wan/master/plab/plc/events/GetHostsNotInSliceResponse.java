package se.sics.kompics.wan.master.plab.plc.events;


import se.sics.kompics.Response;
import se.sics.kompics.wan.master.plab.plc.PlanetLabHost;

public class GetHostsNotInSliceResponse extends Response {

	private final PlanetLabHost[] hosts;
	
	public GetHostsNotInSliceResponse(GetHostsNotInSliceRequest request, PlanetLabHost[] hosts) {
		super(request);
		this.hosts = hosts;
	}
	
	public PlanetLabHost[] getHosts() {
		return hosts;
	}
}
