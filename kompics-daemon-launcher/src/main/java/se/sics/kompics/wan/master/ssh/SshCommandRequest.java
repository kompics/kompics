package se.sics.kompics.wan.master.ssh;

import se.sics.kompics.Request;
import ch.ethz.ssh2.Session;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class SshCommandRequest extends Request {

	private final String command;
	
	private final int sessionId;
	
	private final double timeout;
	
	private final boolean stopOnError;
	
	public SshCommandRequest(int sessionId, String command,
			double timeout, boolean stopOnError) {
		this.command = command;
		this.sessionId = sessionId;
		this.timeout = timeout;
		this.stopOnError = stopOnError;
	}

	/**
	 * @return the command
	 */
	public String getCommand() {
		return command;
	}
	

	/**
	 * @return the sessionId
	 */
	public int getSessionId() {
		return sessionId;
	}
	
	/**
	 * @return the timeout
	 */
	public double getTimeout() {
		return timeout;
	}
	
	/**
	 * @return the stopOnError
	 */
	public boolean isStopOnError() {
		return stopOnError;
	}
}
