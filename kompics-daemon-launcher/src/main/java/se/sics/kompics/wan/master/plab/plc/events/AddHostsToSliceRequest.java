package se.sics.kompics.wan.master.plab.plc.events;


import se.sics.kompics.Request;

public class AddHostsToSliceRequest extends Request {

	private final String[] hosts;
	public AddHostsToSliceRequest(String[] hosts) {
		this.hosts = hosts;
	}
	
	public String[] getHosts() {
		return hosts;
	}
}
