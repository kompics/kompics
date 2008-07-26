package se.sics.kompics.p2p.chord.events;

import java.math.BigInteger;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

/**
 * The <code>ChordLookupRequest</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: ChordLookupRequest.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class ChordLookupRequest implements Event {

	private final BigInteger key;

	public ChordLookupRequest(BigInteger key) {
		super();
		this.key = key;
	}

	public BigInteger getKey() {
		return key;
	}
}
