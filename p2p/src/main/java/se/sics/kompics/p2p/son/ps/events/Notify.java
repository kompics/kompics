package se.sics.kompics.p2p.son.ps.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;

/**
 * The <code>Notify</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: Notify.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class Notify extends PerfectNetworkDeliverEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1542300817463391804L;

	private final Address fromPeer;

	public Notify(Address fromPeer) {
		super();
		this.fromPeer = fromPeer;
	}

	public Address getFromPeer() {
		return fromPeer;
	}
}
