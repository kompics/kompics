package se.sics.kompics.wan.ssh.events;

import java.util.UUID;

import se.sics.kompics.Request;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class SshCommandRequest extends Request {

	protected final UUID requestId;
	
	protected final String command;
	
	protected final int sessionId;
	
	protected final double timeout;
	
	protected final boolean stopOnError;
	
	public SshCommandRequest(UUID requestId, int sessionId, String command,
			double timeout, boolean stopOnError) {
		this.requestId = requestId;
		this.command = command;
		this.sessionId = sessionId;
		this.timeout = timeout;
		this.stopOnError = stopOnError;
	}
	
	public UUID getRequestId() {
		return requestId;
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
