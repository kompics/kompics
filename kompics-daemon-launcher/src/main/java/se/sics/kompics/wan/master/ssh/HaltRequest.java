package se.sics.kompics.wan.master.ssh;

import se.sics.kompics.Request;
import ch.ethz.ssh2.Session;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class HaltRequest extends Request {

	private final Session session;

	public HaltRequest(Session session) {
		this.session = session;
	}

	public Session getSession() {
		return session;
	}
}
