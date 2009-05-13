package se.sics.kompics.kdld.daemon;

import se.sics.kompics.address.Address;

public final class ConnectControllerRequest extends DaemonResponseMessage {

	private static final long serialVersionUID = -1590276498077820239L;

	public ConnectControllerRequest(DaemonAddress src, Address dest) {
		super(src, dest);
	}

}