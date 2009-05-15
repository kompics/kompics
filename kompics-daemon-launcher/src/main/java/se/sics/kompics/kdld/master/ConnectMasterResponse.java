package se.sics.kompics.kdld.master;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

public final class ConnectMasterResponse extends Message {

	private static final long serialVersionUID = -1590276498077820239L;

	private final int timeout;

	private final boolean succeeded;
	
	public ConnectMasterResponse(boolean succeeded, int timeout, Address src, Address dest) {
		super(src, dest);
		this.succeeded = succeeded;
		this.timeout = timeout;
	}

	public int getTimeout() {
		return timeout;
	}
	
	public boolean isSucceeded() {
		return succeeded;
	}
}