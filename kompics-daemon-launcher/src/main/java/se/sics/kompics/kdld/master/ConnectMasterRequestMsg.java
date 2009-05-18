package se.sics.kompics.kdld.master;

import java.util.UUID;

import se.sics.kompics.address.Address;
import se.sics.kompics.kdld.daemon.DaemonAddress;
import se.sics.kompics.network.Message;

public final class ConnectMasterRequestMsg extends Message {

	private static final long serialVersionUID = -1590276498077820239L;

	private final int daemonId;
	
	private final UUID requestId;
	
	public ConnectMasterRequestMsg(UUID requestId, DaemonAddress src, Address dest) {
		super(src.getPeerAddress(), dest);
		this.requestId = requestId;
		this.daemonId = src.getDaemonId();
	}

	public UUID getRequestId() {
		return requestId;
	}
	
	public int getDaemonId() {
		return daemonId;
	}
}