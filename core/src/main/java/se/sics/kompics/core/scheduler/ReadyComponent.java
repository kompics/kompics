package se.sics.kompics.core.scheduler;

import se.sics.kompics.api.Priority;
import se.sics.kompics.core.ComponentCore;

public class ReadyComponent implements Runnable, Comparable<ReadyComponent> {

	private ComponentCore component;

	private int highReadyEventCount;

	private int mediumReadyEventCount;

	private int lowReadyEventCount;

	private Priority scheduledPriority;

	private Priority publishedPriority;

	private Priority executedPriority;

	public ReadyComponent(ComponentCore component, int high, int medium,
			int low, Priority publishedPriority, Priority executedPriority) {
		super();
		this.component = component;
		this.highReadyEventCount = high;
		this.mediumReadyEventCount = medium;
		this.lowReadyEventCount = low;
		this.scheduledPriority = null;
		this.publishedPriority = publishedPriority;
		this.executedPriority = executedPriority;
	}

	public void run() {
		component.schedule(scheduledPriority);
	}

	public void setScheduledPriority(Priority scheduledPriority) {
		this.scheduledPriority = scheduledPriority;
	}

	public Priority getPublishedPriority() {
		return publishedPriority;
	}

	public Priority getExecutedPriority() {
		return executedPriority;
	}

	public int getHighReadyEventCount() {
		return highReadyEventCount;
	}

	public int getMediumReadyEventCount() {
		return mediumReadyEventCount;
	}

	public int getLowReadyEventCount() {
		return lowReadyEventCount;
	}

	public boolean hasHighEventsReady() {
		return highReadyEventCount > 0;
	}

	public boolean hasMediumEventsReady() {
		return mediumReadyEventCount > 0;
	}

	public boolean hasLowEventsReady() {
		return lowReadyEventCount > 0;
	}

	public int compareTo(ReadyComponent that) {
		return this.scheduledPriority.compareTo(that.scheduledPriority);
	}

	public String toString() {
		return "" + scheduledPriority;
	}
}
