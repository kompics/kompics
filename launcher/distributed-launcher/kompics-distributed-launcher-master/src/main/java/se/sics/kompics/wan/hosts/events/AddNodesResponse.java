package se.sics.kompics.wan.hosts.events;


import se.sics.kompics.Response;

public class AddNodesResponse extends Response {

	private final boolean hostsStatus;
	
	public AddNodesResponse(AddNodesRequest request, boolean hostStatus) {
		super(request);
		this.hostsStatus = hostStatus;
	}
	

	public boolean getHostStatus() {
		return hostsStatus;
	}
}
