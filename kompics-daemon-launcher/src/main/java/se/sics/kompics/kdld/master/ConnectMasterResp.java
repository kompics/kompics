package se.sics.kompics.kdld.master;

import se.sics.kompics.Event;
import se.sics.kompics.address.Address;

public final class ConnectMasterResp extends Event {

	private final Address address;
	
	private final int jobId;

	public ConnectMasterResp(int jobId, Address address) {
		super();
		this.jobId = jobId;
		this.address = address;
	}

	public Address getAddress() {
		return address;
	}
	
	public int getJobId() {
		return jobId;
	}
}
