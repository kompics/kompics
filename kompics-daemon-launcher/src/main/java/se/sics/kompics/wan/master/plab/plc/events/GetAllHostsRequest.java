package se.sics.kompics.wan.master.plab.plc.events;


import se.sics.kompics.Request;

public class GetAllHostsRequest extends Request {

	private final boolean bootedOnlyHosts;
	
	public GetAllHostsRequest(boolean bootedOnlyHosts) {
		this.bootedOnlyHosts = bootedOnlyHosts;		
	}
	
	public boolean isBootedOnlyHosts() {
		return bootedOnlyHosts;
	}
}
