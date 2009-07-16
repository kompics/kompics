package se.sics.kompics.wan.ssh.events;

import java.util.UUID;

import se.sics.kompics.Response;
import se.sics.kompics.wan.ssh.Host;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class SshConnectResponse extends Response {

	private final int sessionId;

	private final Host hostname;
	
	private final UUID requestId;
	
	public SshConnectResponse(SshConnectRequest request, int sessionId, UUID requestID,
			Host hostname) {
		super(request);
		this.sessionId = sessionId;
		this.requestId = requestID;
		this.hostname = hostname;
	}

	/**
	 * @return the sessionId
	 */
	public int getSessionId() {
		return sessionId;
	}
	
	public Host getHostname() {
		return hostname;
	}
	
	public UUID getRequestId() {
		return requestId;
	}
}
