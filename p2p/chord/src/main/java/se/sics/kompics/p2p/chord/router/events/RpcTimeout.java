package se.sics.kompics.p2p.chord.router.events;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.timer.events.Timeout;

/**
 * The <code>RpcTimeout</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: RpcTimeout.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class RpcTimeout extends Timeout {

	private final Event rpcRequest;

	private final Address peer;

	public RpcTimeout(Event rpcRequest, Address peer) {
		super();
		this.rpcRequest = rpcRequest;
		this.peer = peer;
	}

	public Event getRpcRequest() {
		return rpcRequest;
	}

	public Address getPeer() {
		return peer;
	}
}
