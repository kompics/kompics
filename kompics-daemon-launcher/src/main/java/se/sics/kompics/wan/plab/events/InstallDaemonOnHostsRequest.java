package se.sics.kompics.wan.plab.events;


import java.util.ArrayList;
import java.util.List;

import se.sics.kompics.Request;
import se.sics.kompics.wan.plab.PLabHost;

public class InstallDaemonOnHostsRequest extends Request {

	private final List<PLabHost> hosts;
	public InstallDaemonOnHostsRequest(List<PLabHost> hosts) {
		this.hosts = new ArrayList<PLabHost>(hosts);
	}
	
	public List<PLabHost> getHosts() {
		return hosts;
	}
}
