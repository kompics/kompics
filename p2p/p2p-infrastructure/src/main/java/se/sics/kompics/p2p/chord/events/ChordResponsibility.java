package se.sics.kompics.p2p.chord.events;

import java.math.BigInteger;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

/**
 * The <code>ChordResponsibility</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: ChordResponsibility.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class ChordResponsibility implements Event {

	// excluded
	private final BigInteger begin;

	// included
	private final BigInteger end;

	public ChordResponsibility(BigInteger begin, BigInteger end) {
		this.begin = begin;
		this.end = end;
	}

	public BigInteger getBegin() {
		return begin;
	}

	public BigInteger getEnd() {
		return end;
	}
}
