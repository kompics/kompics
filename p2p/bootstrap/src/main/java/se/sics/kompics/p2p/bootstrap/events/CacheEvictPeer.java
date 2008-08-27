package se.sics.kompics.p2p.bootstrap.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.timer.events.Alarm;

/**
 * The <code>CacheEvictPeer</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public final class CacheEvictPeer extends Alarm {

	private final Address peerAddress;

	private final long epoch;

	public CacheEvictPeer(Address peerAddress, long epoch) {
		this.peerAddress = peerAddress;
		this.epoch = epoch;
	}

	public Address getPeerAddress() {
		return peerAddress;
	}

	public long getEpoch() {
		return epoch;
	}
}
