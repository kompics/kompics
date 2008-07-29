package se.sics.kompics.p2p.chord.events;

import java.math.BigInteger;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;

/**
 * The <code>ChordLookupFailed</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: ChordLookupFailed.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class ChordLookupFailed implements Event {

	private final BigInteger key;

	private final Object attachment;

	private final Address suspectedPeer;

	public ChordLookupFailed(BigInteger key, Object attachment,
			Address suspectedPeer) {
		super();
		this.key = key;
		this.attachment = attachment;
		this.suspectedPeer = suspectedPeer;
	}

	public BigInteger getKey() {
		return key;
	}

	public Object getAttachment() {
		return attachment;
	}

	public Address getSuspectedPeer() {
		return suspectedPeer;
	}
}
