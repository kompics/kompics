package se.sics.kompics.p2p.fd.events;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Event;

/**
 * The <code>StatusRequest</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: StatusRequest.java 294 2006-05-05 17:14:14Z roberto $
 */
public final class StatusRequest implements Event {

	private final Channel channel;

	public StatusRequest(Channel channel) {
		this.channel = channel;
	}

	public Channel getChannel() {
		return channel;
	}
}
