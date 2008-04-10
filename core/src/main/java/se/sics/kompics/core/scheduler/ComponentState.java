package se.sics.kompics.core.scheduler;

public enum ComponentState {

	/**
	 * the component has at least one event to execute and is either executing
	 * or awaiting execution
	 */
	AWAKE,

	/**
	 * the component has no event to execute
	 */
	ASLEEP;
}
