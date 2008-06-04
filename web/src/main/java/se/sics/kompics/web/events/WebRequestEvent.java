package se.sics.kompics.web.events;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;

@EventType
public final class WebRequestEvent implements Event {

	private final long id;

	private final Address destination;

	private final String request;

	public WebRequestEvent(Address destination, long id, String request) {
		super();
		this.destination = destination;
		this.id = id;
		this.request = request;
	}

	public long getId() {
		return id;
	}

	public Address getDestination() {
		return destination;
	}

	public String getRequest() {
		return request;
	}
}
