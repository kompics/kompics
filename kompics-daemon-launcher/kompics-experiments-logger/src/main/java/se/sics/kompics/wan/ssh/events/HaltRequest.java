package se.sics.kompics.wan.ssh.events;

import se.sics.kompics.Request;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class HaltRequest extends Request {

	private final int sessionId;

	public HaltRequest(int sessionId) {
		this.sessionId = sessionId;
	}

	public int getSessionId() {
		return sessionId;
	}
}
