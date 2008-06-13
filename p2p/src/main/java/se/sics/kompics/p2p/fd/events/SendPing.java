package se.sics.kompics.p2p.fd.events;

import java.math.BigInteger;

import se.sics.kompics.timer.events.TimerSignalEvent;

/**
 * The <code>SendPing</code> class
 * 
 * @author Cosmin Arad
 * @author Roberto Roverso
 * @version $Id: SendPing.java 491 2007-12-11 12:01:50Z roberto $
 */
public final class SendPing extends TimerSignalEvent {

	private final BigInteger peerId;

	public SendPing(BigInteger peerId) {
		this.peerId = peerId;
	}

	public BigInteger getPeerId() {
		return peerId;
	}
}
