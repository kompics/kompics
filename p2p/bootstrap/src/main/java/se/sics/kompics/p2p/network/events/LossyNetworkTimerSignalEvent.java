package se.sics.kompics.p2p.network.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.events.NetworkSendEvent;
import se.sics.kompics.timer.events.TimerSignalEvent;

/**
 * The <code>LossyNetworkTimerSignalEvent</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public final class LossyNetworkTimerSignalEvent extends TimerSignalEvent {

	private final NetworkSendEvent networkSendEvent;

	public LossyNetworkTimerSignalEvent(NetworkSendEvent networkSendEvent) {
		super();
		this.networkSendEvent = networkSendEvent;
	}

	public final NetworkSendEvent getNetworkSendEvent() {
		return networkSendEvent;
	}
}
