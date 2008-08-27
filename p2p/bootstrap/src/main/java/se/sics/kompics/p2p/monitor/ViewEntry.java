package se.sics.kompics.p2p.monitor;

import se.sics.kompics.network.Address;

/**
 * The <code>ViewEntry</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id: ViewEntry.java 142 2008-06-04 15:10:22Z cosmin $
 */
public class ViewEntry {

	private final Address address;

	private long refreshedAt;

	private long evictionTimerId;

	private final long addedAt;

	public ViewEntry(Address address, long now, long addedAt) {
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
