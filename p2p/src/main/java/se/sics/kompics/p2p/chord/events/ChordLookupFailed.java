package se.sics.kompics.p2p.chord.events;

import java.math.BigInteger;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

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

	public ChordLookupFailed(BigInteger key, Object attachment) {
		super();
		this.key = key;
		this.attachment = attachment;
	}

	public BigInteger getKey() {
		return key;
	}

	public Object getAttachment() {
		return attachment;
	}
}
