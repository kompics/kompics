package se.sics.kompics.kdld.master;

import se.sics.kompics.address.Address;
import se.sics.kompics.kdld.daemon.DaemonAddress;
import se.sics.kompics.network.Message;

public final class DisconnectMasterRequest extends Message {

	private static final long serialVersionUID = -2063210982343514311L;

	private final int daemonId;
	
	public DisconnectMasterRequest(DaemonAddress src, Address dest) {
		super(src.getPeerAddress(), dest);
		this.daemonId = src.getDaemonId();
	}

	public int getDaemonId() {
		return daemonId;
	}
}