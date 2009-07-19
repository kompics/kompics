package se.sics.kompics.wan.ssh.scp.events;

import se.sics.kompics.Event;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class ScpGetFinished extends Event {

	private final int commandId;
	public ScpGetFinished(int commandId) {
		this.commandId = commandId;
	}
	
	public int getCommandId() {
		return commandId;
	}
}
