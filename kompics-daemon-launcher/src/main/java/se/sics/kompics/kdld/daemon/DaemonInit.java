package se.sics.kompics.kdld.daemon;

import se.sics.kompics.Init;
import se.sics.kompics.address.Address;

public final class DaemonInit extends Init {

	private final int id;

	private final Address self;

	private final Address masterAddr;

	private final long masterRetryPeriod;

	private final int masterRetryCount;
	
	private final long indexingPeriod;
	
	private final long cacheEvictAfter;

	private final long clientKeepAlivePeriod;
	
	private final int clientWebPort;

	
	/**
	 * 
	 * Called by Client
	 * @param id
	 * @param self
	 * @param masterAddr
	 * @param masterRetryPeriod
	 * @param masterRetryCount
	 * @param indexingPeriod
	 */
	public DaemonInit(int id, Address self, Address masterAddr, long masterRetryPeriod,
			int masterRetryCount, long indexingPeriod) {
		super();
		this.id = id;
		this.self = self;
		if (masterAddr != null) {
			this.masterAddr = masterAddr;
		} else {
			throw new IllegalArgumentException("Master address was null");
		}
		this.masterRetryPeriod = masterRetryPeriod;
		this.masterRetryCount = masterRetryCount;
		this.indexingPeriod = indexingPeriod;
		
		this.cacheEvictAfter = -1;
		this.clientKeepAlivePeriod = -1;
		this.clientWebPort = -1;
	}
	
	/**
	 * Called by Server
	 * @param id
	 * @param self
	 * @param masterAddr
	 * @param masterRetryPeriod
	 * @param masterRetryCount
	 * @param indexingPeriod
	 * @param cacheEvictAfter
	 * @param clientKeepAlivePeriod
	 * @param clientWebPort
	 */
	public DaemonInit(int id, Address self, Address masterAddr, int masterRetryPeriod,
			int masterRetryCount, long indexingPeriod, long cacheEvictAfter, 
			long clientKeepAlivePeriod, int clientWebPort) {
		super();
		this.id = id;
		this.self = self;
		if (masterAddr != null) {
			this.masterAddr = masterAddr;
		} else {
			throw new IllegalArgumentException("Master address was null");
		}
		this.masterRetryPeriod = masterRetryPeriod;
		this.masterRetryCount = masterRetryCount;
		this.indexingPeriod = indexingPeriod;
		
		this.cacheEvictAfter = cacheEvictAfter;
		this.clientKeepAlivePeriod = clientKeepAlivePeriod;
		this.clientWebPort = clientWebPort;
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

	public long getMasterRetryPeriod() {
		return masterRetryPeriod;
	}

	public int getMasterRetryCount() {
		return masterRetryCount;
	}
	
	public long getIndexingPeriod() {
		return indexingPeriod;
	}
	
	public long getCacheEvictAfter() {
		return cacheEvictAfter;
	}
	
	public long getClientKeepAlivePeriod() {
		return clientKeepAlivePeriod;
	}
	
	public int getClientWebPort() {
		return clientWebPort;
	}
}