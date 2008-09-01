package se.sics.kompics.p2p.network.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;

/**
 * The <code>PerfectNetworkMessage</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public final class PerfectNetworkMessage extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8643580210881035448L;

	private final Message message;

	public PerfectNetworkMessage(Message message, Address source,
			Address destination) {
		super(source, destination);
		this.message = message;
	}

	public final Message getMessage() {
		return message;
	}
}
