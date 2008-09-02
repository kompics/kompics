package se.sics.kompics.p2p.fd.events;

import se.sics.kompics.network.Address;
import se.sics.kompics.timer.events.Timeout;

/**
 * The <code>SendPing</code> class
 * 
 * @author Cosmin Arad
 * @author Roberto Roverso
 * @version $Id: SendPing.java 491 2007-12-11 12:01:50Z roberto $
 */
public final class SendPing extends Timeout {

	private final Address peer;

	public SendPing(Address peer) {
		this.peer = peer;
	}

	public Address getPeer() {
		return peer;
	}
}
