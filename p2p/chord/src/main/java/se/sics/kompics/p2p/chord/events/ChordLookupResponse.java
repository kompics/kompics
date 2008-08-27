package se.sics.kompics.p2p.chord.events;

import java.math.BigInteger;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.p2p.chord.router.LookupInfo;

/**
 * The <code>ChordLookupResponse</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: ChordLookupResponse.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class ChordLookupResponse implements Event {

	private final BigInteger key;

	private final Address responsible;

	private final Object attachment;

	private final LookupInfo lookupInfo;

	public ChordLookupResponse(BigInteger key, Address responsible,
			Object attachment, LookupInfo lookupInfo) {
		super();
		this.key = key;
		this.responsible = responsible;
		this.attachment = attachment;
		this.lookupInfo = lookupInfo;
	}

	public BigInteger getKey() {
		return key;
	}

	public Address getResponsible() {
		return responsible;
	}

	public Object getAttachment() {
		return attachment;
	}

	public LookupInfo getLookupInfo() {
		return lookupInfo;
	}
}
