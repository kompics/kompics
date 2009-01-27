package se.sics.kompics.p2p.bootstrap;

import se.sics.kompics.network.Address;

/**
 * The <code>CacheEntry</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
public class CacheEntry {

	private final Address address;

	private long refreshedAt;

	private long evictionTimerId;

	private final long addedAt;

	public CacheEntry(Address address, long now, long addedAt) {
		this.address = address;
		this.refreshedAt = now;
		this.addedAt = addedAt;
	}

	public Address getAddress() {
		return address;
	}

	public long getRefreshedAt() {
		return refreshedAt;
	}

	public void setRefreshedAt(long refreshedAt) {
		this.refreshedAt = refreshedAt;
	}

	public long getAddedAt() {
		return addedAt;
	}

	public long getEvictionTimerId() {
		return evictionTimerId;
	}

	public void setEvictionTimerId(long evictionTimerId) {
		this.evictionTimerId = evictionTimerId;
	}
}
