package se.sics.kompics.p2p.fd.events;

import se.sics.kompics.network.Address;
import se.sics.kompics.timer.events.Alarm;

/**
 * The <code>PongTimedOut</code> class
 * 
 * @author Cosmin Arad
 * @author Roberto Roverso
 * @version $Id: PongTimedOut.java 491 2007-12-11 12:01:50Z roberto $
 */
public final class PongTimedOut extends Alarm {

	private final Address peer;

	public PongTimedOut(Address peer) {
		this.peer = peer;
	}

	public Address getPeer() {
		return peer;
	}
}
