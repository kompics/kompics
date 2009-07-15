package se.sics.kompics.wan.plab.events;


import se.sics.kompics.Response;
import se.sics.kompics.wan.plab.PLabHost;

public class InstallDaemonOnHostsResponse extends Response {

	private final PLabHost[] hosts;
	
	public InstallDaemonOnHostsResponse(InstallDaemonOnHostsRequest request, PLabHost[] hosts) {
		super(request);
		this.hosts = hosts;
	}
	
	public PLabHost[] getHosts() {
		return hosts;
	}
}
