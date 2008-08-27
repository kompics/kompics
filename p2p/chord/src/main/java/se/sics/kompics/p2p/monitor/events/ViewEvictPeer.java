package se.sics.kompics.p2p.monitor.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.timer.events.TimerSignalEvent;

/**
 * The <code>ViewEvictPeer</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id: ViewEvictPeer.java 142 2008-06-04 15:10:22Z cosmin $
 */
@EventType
public final class ViewEvictPeer extends TimerSignalEvent {

	private final Address peerAddress;

	public ViewEvictPeer(Address peerAddress) {
		this.peerAddress = peerAddress;
	}

	public Address getPeerAddress() {
		return peerAddress;
	}
}
