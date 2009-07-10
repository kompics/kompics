package se.sics.kompics.wan.plab.events;


import java.util.ArrayList;
import java.util.List;

import se.sics.kompics.Request;
import se.sics.kompics.wan.master.plab.plc.PlanetLabHost;

public class GetHostsNotInSliceRequest extends Request {

	private final List<PlanetLabHost> hosts;
	public GetHostsNotInSliceRequest(List<PlanetLabHost> hosts) {
		this.hosts = new ArrayList<PlanetLabHost>(hosts);
	}
	
	public List<PlanetLabHost> getHosts() {
		return hosts;
	}
}
