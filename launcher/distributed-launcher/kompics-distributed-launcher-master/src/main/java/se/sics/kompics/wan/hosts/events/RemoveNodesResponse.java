package se.sics.kompics.wan.hosts.events;


import se.sics.kompics.Response;

public class RemoveNodesResponse extends Response {

	private final boolean hostsStatus;
	
	public RemoveNodesResponse(RemoveNodesRequest request, boolean hostStatus) {
		super(request);
		this.hostsStatus = hostStatus;
	}
	

	public boolean getHostStatus() {
		return hostsStatus;
	}
}
