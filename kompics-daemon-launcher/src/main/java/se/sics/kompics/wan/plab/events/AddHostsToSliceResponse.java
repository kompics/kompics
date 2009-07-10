package se.sics.kompics.wan.plab.events;


import java.util.Map;

import se.sics.kompics.Response;

public class AddHostsToSliceResponse extends Response {

	private final Map<String, Boolean> hostStatus;
	
	public AddHostsToSliceResponse(AddHostsToSliceRequest request, Map<String, Boolean> hostStatus) {
		super(request);
		this.hostStatus = hostStatus;
	}
	

	public Map<String, Boolean> getHostStatus() {
		return hostStatus;
	}
}
