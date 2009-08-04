package org.component.chord.web.application;

import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.ScheduleTimeout;

public final class ComponentTimeout extends Timeout {

	private final int attribute;

	public ComponentTimeout(ScheduleTimeout request, int attribute) {
		super(request);
		this.attribute = attribute;
	}
	
	public int getAttribute() {
		return attribute;
	}
}
