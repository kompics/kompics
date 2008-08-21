package se.sics.kompics.management;

import java.beans.ConstructorProperties;

public class EventCounter implements Comparable<EventCounter> {

	public final long count;

	public final String event;

	@ConstructorProperties( { "count", "event" })
	public EventCounter(long count, String event) {
		this.count = count;
		this.event = event;
	}

	public long getCount() {
		return count;
	}

	public String getEvent() {
		return event;
	}

	public int compareTo(EventCounter that) {
		if (this.count < that.count)
			return 1;
		if (this.count > that.count)
			return -1;
		return -this.event.compareTo(that.event);
	}
}
