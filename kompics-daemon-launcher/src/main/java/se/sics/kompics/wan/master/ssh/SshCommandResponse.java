package se.sics.kompics.wan.master.ssh;

import se.sics.kompics.Response;
import ch.ethz.ssh2.Session;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class SshCommandResponse extends Response {

	private final String commandResponse;
	
	private final Session session;
	
	private final boolean status;
	
	public SshCommandResponse(SshCommandRequest request, Session session, String commandResponse,
			 boolean status) {
		super(request);
		this.commandResponse = commandResponse;
		this.session = session;
		this.status = status;
	}

	/**
	 * @return the command
	 */
	public String getCommandResponse() {
		return commandResponse;
	}
	
	/**
	 * @return the session
	 */
	public Session getSession() {
		return session;
	}
	
	/**
	 * @return the stopOnError
	 */
	public boolean isStatus() {
		return status;
	}
}
