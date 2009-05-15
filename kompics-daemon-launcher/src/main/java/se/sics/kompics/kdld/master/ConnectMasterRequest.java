package se.sics.kompics.kdld.master;

import se.sics.kompics.address.Address;
import se.sics.kompics.kdld.daemon.DaemonAddress;
import se.sics.kompics.network.Message;

public final class ConnectMasterRequest extends Message {

	private static final long serialVersionUID = -1590276498077820239L;

	final int daemonId;
	
	public ConnectMasterRequest(DaemonAddress src, Address dest) {
		super(src.getPeerAddress(), dest);
		this.daemonId = src.getDaemonId();
	}

	public int getDaemonId() {
		return daemonId;
	}
}