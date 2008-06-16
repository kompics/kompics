package se.sics.kompics.p2p.son.ps.events;

import java.math.BigInteger;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;

/**
 * The <code>FindSuccessorRequest</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: FindSuccessorRequest.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class FindSuccessorRequest extends PerfectNetworkDeliverEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4476609047322811330L;

	private final BigInteger identifier;

	public FindSuccessorRequest(BigInteger identifier) {
		super();
		this.identifier = identifier;
	}

	public BigInteger getIdentifier() {
		return identifier;
	}
}
