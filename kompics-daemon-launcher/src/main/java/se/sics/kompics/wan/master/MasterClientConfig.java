package se.sics.kompics.wan.master;

import se.sics.kompics.address.Address;

public final class MasterClientConfig {

	private final Address masterAddress;
	
	private final long clientRetryPeriod;

	private final int clientRetryCount;

	private final long clientKeepAlivePeriod;
	
	public MasterClientConfig(Address masterAddress,
			long clientRetryPeriod, int clientRetryCount,
			long clientKeepAlivePeriod) {
		this.masterAddress = masterAddress;
		this.clientRetryPeriod = clientRetryPeriod;
		this.clientRetryCount = clientRetryCount;
		this.clientKeepAlivePeriod = clientKeepAlivePeriod;
	}

	public Address getMasterAddress() {
		return masterAddress;
	}

	public long getClientRetryPeriod() {
		return clientRetryPeriod;
	}

	public int getClientRetryCount() {
		return clientRetryCount;
	}

	public long getClientKeepAlivePeriod() {
		return clientKeepAlivePeriod;
	}

}