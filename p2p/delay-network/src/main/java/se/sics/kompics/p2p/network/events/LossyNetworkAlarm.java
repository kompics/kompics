package se.sics.kompics.p2p.network.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.events.Message;
import se.sics.kompics.timer.events.Timeout;

/**
 * The <code>LossyNetworkAlarm</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public final class LossyNetworkAlarm extends Timeout {

	private final Message message;

	public LossyNetworkAlarm(Message message) {
		super();
		this.message = message;
	}

	public final Message getMessage() {
		return message;
	}
}
