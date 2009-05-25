package se.sics.kompics.wan.master;

import se.sics.kompics.address.Address;

public final class MasterConfiguration {

	private final Address masterAddress;
	
	private final long cacheEvictAfter;
	
	private final long clientRetryPeriod;

	private final int clientRetryCount;

	private final long clientKeepAlivePeriod;
	
	private final int clientWebPort;

	public MasterConfiguration(Address masterAddress,
			long cacheEvictAfter, long clientRetryPeriod, int clientRetryCount,
			long clientKeepAlivePeriod, int clientWebPort) {
		this.masterAddress = masterAddress;
		this.cacheEvictAfter = cacheEvictAfter;
		this.clientRetryPeriod = clientRetryPeriod;
		this.clientRetryCount = clientRetryCount;
		this.clientKeepAlivePeriod = clientKeepAlivePeriod;
		this.clientWebPort = clientWebPort;
	}

	public Address getMasterAddress() {
		return masterAddress;
	}

	public long getCacheEvictAfter() {
		return cacheEvictAfter;
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

	public int getClientWebPort() {
		return clientWebPort;
	}
}