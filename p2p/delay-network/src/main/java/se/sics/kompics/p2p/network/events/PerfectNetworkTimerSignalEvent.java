package se.sics.kompics.p2p.network.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.events.Message;
import se.sics.kompics.timer.events.TimerSignalEvent;

/**
 * The <code>PerfectNetworkTimerSignalEvent</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id: PerfectNetworkTimerSignalEvent.java 139 2008-06-04 10:55:59Z
 *          cosmin $
 */
@EventType
public final class PerfectNetworkTimerSignalEvent extends TimerSignalEvent {

	private final Message networkSendEvent;

	public PerfectNetworkTimerSignalEvent(Message networkSendEvent) {
		super();
		this.networkSendEvent = networkSendEvent;
	}

	public final Message getNetworkSendEvent() {
		return networkSendEvent;
	}
}
