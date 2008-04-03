package se.sics.kompics.core.sched;

import se.sics.kompics.api.Priority;
import se.sics.kompics.core.ComponentCore;

public class ReadyComponent implements Runnable, Prioritizable {

	private ComponentCore component;
	
	private Priority priority;

	public ReadyComponent(ComponentCore component, Priority priority) {
		super();
		this.component = component;
		this.priority = priority;
	}

	public void run() {
		component.schedule(priority);
	}

	public Priority getPriority() {
		return priority;
	}
}
