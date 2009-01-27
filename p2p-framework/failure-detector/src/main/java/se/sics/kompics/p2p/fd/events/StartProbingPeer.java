package se.sics.kompics.p2p.fd.events;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Event;
import se.sics.kompics.network.Address;

/**
 * The <code>StartProbingPeer</code> class
 * 
 * @author Cosmin Arad
 * @author Roberto Roverso
 * @version $Id: StartProbingPeer.java 294 2006-05-05 17:14:14Z roberto $
 */
public final class StartProbingPeer implements Event {

	private final Address peerAddress;

	private final Component component;

	private final Channel channel;

	public StartProbingPeer(Address peerAddress, Component component,
			Channel channel) {
		this.peerAddress = peerAddress;
		this.component = component;
		this.channel = channel;
	}

	public Address getPeerAddress() {
		return peerAddress;
	}

	public Component getComponent() {
		return component;
	}

	public Channel getChannel() {
		return channel;
	}
}
