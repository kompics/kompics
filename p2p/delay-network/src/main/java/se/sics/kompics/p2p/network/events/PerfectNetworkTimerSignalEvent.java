package se.sics.kompics.p2p.network.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.events.NetworkSendEvent;
import se.sics.kompics.timer.events.TimerSignalEvent;

/**
 * The <code>PerfectNetworkTimerSignalEvent</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public final class PerfectNetworkTimerSignalEvent extends TimerSignalEvent {

	private final NetworkSendEvent networkSendEvent;

	public PerfectNetworkTimerSignalEvent(NetworkSendEvent networkSendEvent) {
		super();
		this.networkSendEvent = networkSendEvent;
	}

	public final NetworkSendEvent getNetworkSendEvent() {
		return networkSendEvent;
	}
}
