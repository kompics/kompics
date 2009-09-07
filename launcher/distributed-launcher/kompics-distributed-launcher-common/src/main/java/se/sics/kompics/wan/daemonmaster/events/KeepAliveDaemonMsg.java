package se.sics.kompics.wan.daemonmaster.events;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.wan.masterdaemon.events.DaemonAddress;


public final class KeepAliveDaemonMsg extends Message {


	private static final long serialVersionUID = -7890367769241722616L;

	private final DaemonAddress peerAddress;

	public KeepAliveDaemonMsg(DaemonAddress peerAddress, Address destination) {
		super(peerAddress.getPeerAddress(), destination);
		this.peerAddress = peerAddress;
	}

	public DaemonAddress getPeerAddress() {
		return peerAddress;
	}
	
}