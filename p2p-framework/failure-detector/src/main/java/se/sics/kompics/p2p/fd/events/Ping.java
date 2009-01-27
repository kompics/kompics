package se.sics.kompics.p2p.fd.events;

import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;

/**
 * The <code>Ping</code> class
 * 
 * @author Cosmin Arad
 * @author Roberto Roverso
 * @version $Id: Ping.java 294 2006-05-05 17:14:14Z roberto $
 */
public final class Ping extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5066114231916864333L;

	private final long id;

	private final long ts;
	
	public Ping(long id, long ts, Address source, Address destination) {
		super(source, destination);
		this.id = id;
		this.ts =ts;
	}

	public long getId() {
		return id;
	}

	public long getTs() {
		return ts;
	}
}
