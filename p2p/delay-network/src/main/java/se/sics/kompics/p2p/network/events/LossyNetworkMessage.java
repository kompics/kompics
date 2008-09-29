package se.sics.kompics.p2p.network.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;

/**
 * The <code>LossyNetworkMessage</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public final class LossyNetworkMessage extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4037559446497298014L;

	private final Message message;

	public LossyNetworkMessage(Message message, Address source,
			Address destination) {
		super(source, destination);
		this.message = message;
	}

	public final Message getMessage() {
		return message;
	}
}
