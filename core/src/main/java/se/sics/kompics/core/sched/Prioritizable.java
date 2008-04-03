package se.sics.kompics.core.sched;

import se.sics.kompics.api.Priority;

public interface Prioritizable {
	/**
	 * Gets the priority of this entry
	 * 
	 * @return the {@link Priority}
	 */
	public Priority getPriority();
}
