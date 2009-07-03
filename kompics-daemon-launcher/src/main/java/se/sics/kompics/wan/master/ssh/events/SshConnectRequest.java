package se.sics.kompics.wan.master.ssh.events;

import se.sics.kompics.Request;
import se.sics.kompics.wan.master.ssh.Credentials;
import se.sics.kompics.wan.master.ssh.ExperimentHost;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class SshConnectRequest extends Request {

	private final Credentials credentials;
	private final ExperimentHost hostname;
	
	public SshConnectRequest(Credentials credentials, ExperimentHost hostname) {
		this.credentials = credentials;
		this.hostname = hostname;
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
	public ExperimentHost getHostname() {
		return hostname;
	}

}
