package se.sics.kompics.wan.master.ssh;

import se.sics.kompics.Response;
import ch.ethz.ssh2.Session;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class SshConnectResponse extends Response {

	private final Session session;
	
	public SshConnectResponse(SshConnectRequest request, Session session) {
		super(request);
		this.session = session;
	}

	
	/**
	 * @return the session
	 */
	public Session getSession() {
		return session;
	}

}
