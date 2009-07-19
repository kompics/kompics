package se.sics.kompics.wan.ssh.events;

import se.sics.kompics.Response;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class SshHeartbeatResponse extends Response {

	private final int sessionId;

	private final boolean status;

	public SshHeartbeatResponse(SshHeartbeatRequest request, int sessionId,
			boolean status) {
		super(request);
		this.sessionId = sessionId;
		this.status = status;
	}

	/**
	 * @return the sessionId
	 */
	public int getSessionId() {
		return sessionId;
	}

	/**
	 * @return the stopOnError
	 */
	public boolean isStatus() {
		return status;
	}
}
