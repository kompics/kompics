package se.sics.kompics.p2p.chord.events;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

/**
 * The <code>GetChordNeighborsRequest</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: GetChordNeighborsRequest.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class GetChordNeighborsRequest implements Event {

	private final Channel responseChannel;

	public GetChordNeighborsRequest(Channel responseChannel) {
		this.responseChannel = responseChannel;
	}

	public Channel getResponseChannel() {
		return responseChannel;
	}
}
