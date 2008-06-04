package se.sics.kompics.web.events;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

@EventType
public final class WebResponseEvent implements Event {

	private final long id;

	private final String html;

	public WebResponseEvent(String html, long id) {
		this.html = html;
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public String getHtml() {
		return html;
	}
}
