package se.sics.kompics.p2p.monitor.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;

/**
 * The <code>ChangeUpdatePeriod.java</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id: ChangeUpdatePeriod.java.java 142 2008-06-04 15:10:22Z cosmin $
 */
@EventType
public final class ChangeUpdatePeriod extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4876649426093003063L;

	private final long newUpdatePeriod;

	public ChangeUpdatePeriod(long newUpdatePeriod, Address destination) {
		super(destination);
		this.newUpdatePeriod = newUpdatePeriod;
	}

	public long getNewUpdatePeriod() {
		return newUpdatePeriod;
	}
}
