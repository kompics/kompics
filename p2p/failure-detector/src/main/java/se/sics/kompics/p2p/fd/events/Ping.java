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
	private static final long serialVersionUID = 3746724182377458990L;

	private final long id;

	public Ping(long id, Address source, Address destination) {
		super(source, destination);
		this.id = id;
	}

	public long getId() {
		return id;
	}
}
