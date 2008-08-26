package se.sics.kompics.management;

import java.beans.ConstructorProperties;

public class ComponentEventCounter {

	private final long published;

	private final long handled;

	private final String event;

	@ConstructorProperties( { "published", "handled", "event" })
	public ComponentEventCounter(long published, long handled, String event) {
		this.published = published;
		this.handled = handled;
		this.event = event;
	}

	public long getPublished() {
		return published;
	}

	public long getHandled() {
		return handled;
	}

	public String getEvent() {
		return event;
	}
}
