package se.sics.kompics.wan.master.ssh.events;

import se.sics.kompics.Response;
import se.sics.kompics.wan.master.ssh.ExperimentHost;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class SshConnectResponse extends Response {

	private final int sessionId;

	private final ExperimentHost hostname;
	
	public SshConnectResponse(SshConnectRequest request, int sessionId,
			ExperimentHost hostname) {
		super(request);
		this.sessionId = sessionId;
		this.hostname = hostname;
	}

	/**
	 * @return the sessionId
	 */
	public int getSessionId() {
		return sessionId;
	}
	
	public ExperimentHost getHostname() {
		return hostname;
	}
}
