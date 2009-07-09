package se.sics.kompics.wan.master.ssh.events;

import se.sics.kompics.Request;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class SshHeartbeatRequest extends Request {

	protected final int sessionId;
	
	
	public SshHeartbeatRequest(int sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * @return the sessionId
	 */
	public int getSessionId() {
		return sessionId;
	}
	
}
