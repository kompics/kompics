package se.sics.kompics.wan.ssh.events;

import java.util.UUID;

import se.sics.kompics.Request;
import se.sics.kompics.wan.ssh.Credentials;
import se.sics.kompics.wan.ssh.Host;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class SshConnectRequest extends Request {

	private final Credentials credentials;
	private final Host host;
	private final UUID requestId;
	
	public SshConnectRequest(Credentials credentials, UUID requestId, Host hostname) {
		this.credentials = credentials;
		this.host = hostname;
		this.requestId = requestId;
	}

	/**
	 * @return the credentials
	 */
	public Credentials getCredentials() {
		return credentials;
	}

	/**
	 * @return the hostname
	 */
	public Host getHost() {
		return host;
	}

	public UUID getRequestId() {
		return requestId;
	}
	
}
