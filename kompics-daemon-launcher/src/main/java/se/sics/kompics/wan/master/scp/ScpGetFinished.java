package se.sics.kompics.wan.master.scp;

import se.sics.kompics.Event;
import ch.ethz.ssh2.SCPClient;

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
