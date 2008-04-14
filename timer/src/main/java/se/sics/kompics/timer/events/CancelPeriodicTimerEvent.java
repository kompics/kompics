package se.sics.kompics.timer.events;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

/**
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public class CancelPeriodicTimerEvent implements Event {

	private Component clientComponent;

	private long timerId;

	public CancelPeriodicTimerEvent(Component component, long timerId) {
		this.timerId = timerId;
		this.clientComponent = component;
	}

	public Component getClientComponent() {
		return clientComponent;
	}

	public long getTimerId() {
		return timerId;
	}
}
