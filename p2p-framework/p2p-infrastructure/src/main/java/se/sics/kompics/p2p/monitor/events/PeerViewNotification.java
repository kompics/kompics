package se.sics.kompics.p2p.monitor.events;

import java.util.Map;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;

/**
 * The <code>PeerMonitorSendView</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id: PeerMonitorSendView.java 142 2008-06-04 15:10:22Z cosmin $
 */
@EventType
public final class PeerViewNotification extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6744356131493078333L;

	private final Address peerAddress;

	// various data collected from components within a peer
	private final Map<String, Object> peerData;

	public PeerViewNotification(Address peerAddress,
			Map<String, Object> peerData, Address destination) {
		super(destination);
		this.peerAddress = peerAddress;
		this.peerData = peerData;
	}

	public Address getPeerAddress() {
		return peerAddress;
	}

	public Map<String, Object> getPeerData() {
		return peerData;
	}
}
