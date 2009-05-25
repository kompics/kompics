package se.sics.kompics.wan.slave;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

/**
 * The <code>SlaveMessage</code> class.
 * 
 */
public abstract class SlaveMessage extends Message {

	private static final long serialVersionUID = 565200273527489785L;
	
	private final int slaveId;

	public SlaveMessage(int slaveId, Address source, Address destination) {
		super(source, destination);
		this.slaveId = slaveId;
	}

	public int getSlaveId() {
		return slaveId;
	} 

}
