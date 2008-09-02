package se.sics.kompics.p2p.fd.events;

import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;

/**
 * The <code>Pong</code> class
 * 
 * @author Cosmin Arad
 * @author Roberto Roverso
 * @version $Id: Pong.java 294 2006-05-05 17:14:14Z roberto $
 */
public final class Pong extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4264214351542432046L;

	private final long id;

	public Pong(long id, Address source, Address destination) {
		super(source, destination);
		this.id = id;
	}

	public long getId() {
		return id;
	}
}
