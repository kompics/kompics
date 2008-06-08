package se.sics.kompics.web.events;

import java.math.BigInteger;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

/**
 * The <code>WebRequestEvent</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public final class WebRequestEvent implements Event {

	private final long id;

	private final BigInteger destination;

	private final String request;

	public WebRequestEvent(BigInteger destination, long id, String request) {
		super();
		this.destination = destination;
		this.id = id;
		this.request = request;
	}

	public long getId() {
		return id;
	}

	public BigInteger destination() {
		return destination;
	}

	public String getRequest() {
		return request;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((destination == null) ? 0 : destination.hashCode());
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + ((request == null) ? 0 : request.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WebRequestEvent other = (WebRequestEvent) obj;
		if (destination == null) {
			if (other.destination != null)
				return false;
		} else if (!destination.equals(other.destination))
			return false;
		if (id != other.id)
			return false;
		if (request == null) {
			if (other.request != null)
				return false;
		} else if (!request.equals(other.request))
			return false;
		return true;
	}
}
