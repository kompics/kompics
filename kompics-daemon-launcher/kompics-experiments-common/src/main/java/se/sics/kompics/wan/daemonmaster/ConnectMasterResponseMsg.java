package se.sics.kompics.wan.daemonmaster;

import java.util.UUID;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;


public final class ConnectMasterResponseMsg extends Message {

	private static final long serialVersionUID = -1590276498077820239L;


	private final boolean succeeded;
	
	private final UUID requestId;
	
	public ConnectMasterResponseMsg(boolean succeeded, UUID requestId, Address src, Address dest) {
		super(src, dest);
		this.succeeded = succeeded;
		this.requestId = requestId;
	}

	public UUID getRequestId() {
		return requestId;
	}

	public boolean isSucceeded() {
		return succeeded;
	}
}