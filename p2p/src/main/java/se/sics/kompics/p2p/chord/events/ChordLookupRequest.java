package se.sics.kompics.p2p.chord.events;

import java.math.BigInteger;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;

/**
 * The <code>ChordLookupRequest</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: ChordLookupRequest.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class ChordLookupRequest implements Event {

	private final BigInteger key;

	private final Channel responseChannel;

	private final Address firstPeer;

	private final Object attachment;

	public ChordLookupRequest(BigInteger key, Channel responseChannel,
			Object attachment) {
		super();
		this.key = key;
		this.responseChannel = responseChannel;
		this.attachment = attachment;
		this.firstPeer = null;
	}

	public ChordLookupRequest(BigInteger key, Channel responseChannel,
			Object attachment, Address firstPeer) {
		super();
		this.key = key;
		this.responseChannel = responseChannel;
		this.attachment = attachment;
		this.firstPeer = firstPeer;
	}

	public BigInteger getKey() {
		return key;
	}

	public Channel getResponseChannel() {
		return responseChannel;
	}

	public Object getAttachment() {
		return attachment;
	}

	public Address getFirstPeer() {
		return firstPeer;
	}
}
