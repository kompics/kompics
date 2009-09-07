package se.sics.kompics.wan.services.events;


import java.util.List;

import se.sics.kompics.Response;
import se.sics.kompics.wan.services.ExperimentServicesComponent;

public class GetStatusResponse extends Response {

	private final List<String> hosts;
	
	private final List<ExperimentServicesComponent.ServicesStatus> status;
	
	public GetStatusResponse(GetStatusRequest request, List<String> hosts,
			List<ExperimentServicesComponent.ServicesStatus> status) {
		super(request);
		this.hosts = hosts;
		this.status = status;
	}
	
	public List<String> getHosts() {
		return hosts;
	}
	
	public List<ExperimentServicesComponent.ServicesStatus> getStatus() {
		return status;
	}
}
