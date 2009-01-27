package se.sics.kompics.p2p.chord.router.events;

import java.math.BigInteger;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;

/**
 * The <code>FindSuccessorRequest</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: FindSuccessorRequest.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class FindSuccessorRequest extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1423303212221670201L;

	private final BigInteger key;

	private final long lookupId;

	public FindSuccessorRequest(BigInteger key, long lookupId,
			Address destination) {
		super(destination);
		this.key = key;
		this.lookupId = lookupId;
	}

	public BigInteger getKey() {
		return key;
	}

	public long getLookupId() {
		return lookupId;
	}
}
