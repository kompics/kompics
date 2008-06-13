package se.sics.kompics.p2p.fd.events;

import java.math.BigInteger;

import se.sics.kompics.timer.events.TimerSignalEvent;

/**
 * The <code>PongTimedOut</code> class
 * 
 * @author Cosmin Arad
 * @author Roberto Roverso
 * @version $Id: PongTimedOut.java 491 2007-12-11 12:01:50Z roberto $
 */
public final class PongTimedOut extends TimerSignalEvent {

	private final BigInteger peerId;

	public PongTimedOut(BigInteger peerId) {
		this.peerId = peerId;
	}

	public BigInteger getPeerId() {
		return peerId;
	}
}
