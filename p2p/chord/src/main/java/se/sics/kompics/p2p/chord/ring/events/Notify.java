package se.sics.kompics.p2p.chord.ring.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;

/**
 * The <code>Notify</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: Notify.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class Notify extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5676163908643118529L;

	private final Address fromPeer;

	public Notify(Address fromPeer, Address destionation) {
		super(fromPeer, destionation);
		this.fromPeer = fromPeer;
	}

	public Address getFromPeer() {
		return fromPeer;
	}
}
