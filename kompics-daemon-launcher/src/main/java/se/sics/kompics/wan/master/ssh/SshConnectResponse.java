package se.sics.kompics.wan.master.ssh;

import se.sics.kompics.Response;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class SshConnectResponse extends Response {

	private final int sessionId;

	public SshConnectResponse(SshConnectRequest request, int sessionId) {
		super(request);
		this.sessionId = sessionId;
	}

	/**
	 * @return the sessionId
	 */
	public int getSessionId() {
		return sessionId;
	}
}
