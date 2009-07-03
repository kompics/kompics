package se.sics.kompics.wan.master.plab.plc.events;


import se.sics.kompics.Request;
import se.sics.kompics.wan.master.plab.plc.PlanetLabHost;

public class GetHostsNotInSliceRequest extends Request {

	private final PlanetLabHost[] hosts;
	public GetHostsNotInSliceRequest(PlanetLabHost[] hosts) {
		this.hosts = hosts;
	}
	
	public PlanetLabHost[] getHosts() {
		return hosts;
	}
}
