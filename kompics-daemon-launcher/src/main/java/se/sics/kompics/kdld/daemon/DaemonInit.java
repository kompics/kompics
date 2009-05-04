package se.sics.kompics.kdld.daemon;

import se.sics.kompics.Init;
import se.sics.kompics.address.Address;

public final class DaemonInit extends Init {

	private final int id;

	private final Address self;

	private final Address masterAddr;

	private final int masterRetryTimeout;

	private final int masterRetryCount;
	
	private final long indexingPeriod;

	public DaemonInit(int id, Address self, Address masterAddr, int masterRetryTimeout,
			int masterRetryCount, long indexingPeriod) {
		super();
		this.id = id;
		this.self = self;
		if (masterAddr != null) {
			this.masterAddr = masterAddr;
		} else {
			throw new IllegalArgumentException("Master address was null");
		}
		this.masterRetryTimeout = masterRetryTimeout;
		this.masterRetryCount = masterRetryCount;
		this.indexingPeriod = indexingPeriod;
	}

	public int getId() {
		return id;
	}

	public Address getSelf() {
		return self;
	}

	public Address getMasterAddr() {
		return masterAddr;
	}

	public int getMasterRetryTimeout() {
		return masterRetryTimeout;
	}

	public int getMasterRetryCount() {
		return masterRetryCount;
	}
	
	public long getIndexingPeriod() {
		return indexingPeriod;
	}
}