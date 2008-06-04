package se.sics.kompics.p2p.bootstrap.events;

import se.sics.kompics.network.Address;

/**
 * The <code>PeerEntry</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
public class PeerEntry {

	private final Address address;

	private final long age;

	private final long freshness;

	public PeerEntry(Address address, long age, long freshness) {
		this.address = address;
		this.age = age;
		this.freshness = freshness;
	}

	public Address getAddress() {
		return address;
	}

	public long getAge() {
		return age;
	}

	public long getFreshness() {
		return freshness;
	}
}
