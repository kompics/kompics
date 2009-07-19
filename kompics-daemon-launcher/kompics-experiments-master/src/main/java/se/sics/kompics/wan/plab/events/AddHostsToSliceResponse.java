package se.sics.kompics.wan.plab.events;


import se.sics.kompics.Response;

public class AddHostsToSliceResponse extends Response {

	private final boolean hostsStatus;
	
	public AddHostsToSliceResponse(AddHostsToSliceRequest request, boolean hostStatus) {
		super(request);
		this.hostsStatus = hostStatus;
	}
	

	public boolean getHostStatus() {
		return hostsStatus;
	}
}
