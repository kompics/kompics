package se.sics.kompics.wan.master.ssh;

import se.sics.kompics.Event;
import ch.ethz.ssh2.Session;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class SshCommand extends Event {

	private final String command;
	
	private final Session session;
	
	private final double timeout;
	
	private final boolean stopOnError;
	
	public SshCommand(Session session, String command,
			double timeout, boolean stopOnError) {
		this.command = command;
		this.session = session;
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
	 * @return the session
	 */
	public Session getSession() {
		return session;
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
