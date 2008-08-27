package se.sics.kompics.p2p.monitor.events;

import java.math.BigInteger;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.timer.events.Alarm;

/**
 * The <code>SendView</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id: SendView.java 142 2008-06-04 15:10:22Z cosmin $
 */
@EventType
public final class SendView extends Alarm {

	public final BigInteger peerId;

	public SendView(BigInteger peerId) {
		this.peerId = peerId;
	}
}
