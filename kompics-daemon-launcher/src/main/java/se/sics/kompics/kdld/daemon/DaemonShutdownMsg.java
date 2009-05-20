package se.sics.kompics.kdld.daemon;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

/**
 * The <code>Hello</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class DaemonShutdownMsg extends Message {

	private static final long serialVersionUID = -91334132413638L;

	private final int timeout;

	public DaemonShutdownMsg(int timeout, Address src, Address dest) {
		super(src, dest);
		this.timeout = timeout;
	}

	public int getTimeout() {
		return timeout;
	}
}