package se.sics.kompics.p2p.bootstrap.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.timer.events.Alarm;

/**
 * The <code>ClientRetryRequest.java</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id: ClientRetryRequest.java.java 142 2008-06-04 15:10:22Z cosmin $
 */
@EventType
public final class ClientRetryRequest extends Alarm {

	private BootstrapRequest request;

	public ClientRetryRequest(BootstrapRequest request) {
		this.request = request;
	}

	public BootstrapRequest getRequest() {
		return request;
	}
}
