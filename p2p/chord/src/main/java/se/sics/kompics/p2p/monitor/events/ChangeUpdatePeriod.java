package se.sics.kompics.p2p.monitor.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.p2p.network.events.LossyNetworkDeliverEvent;

/**
 * The <code>ChangeUpdatePeriod.java</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id: ChangeUpdatePeriod.java.java 142 2008-06-04 15:10:22Z cosmin $
 */
@EventType
public final class ChangeUpdatePeriod extends LossyNetworkDeliverEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3126611520737955241L;

	private final long newUpdatePeriod;

	public ChangeUpdatePeriod(long newUpdatePeriod) {
		super();
		this.newUpdatePeriod = newUpdatePeriod;
	}

	public long getNewUpdatePeriod() {
		return newUpdatePeriod;
	}
}
