package se.sics.kompics.core.scheduler;

import se.sics.kompics.management.SchedulerMXBean;

public class SchedulerMXBeanImpl implements SchedulerMXBean {

	private Scheduler scheduler;

	public SchedulerMXBeanImpl(Scheduler scheduler) {
		super();
		this.scheduler = scheduler;
	}

	public long getWorkStealingCount() {
		return scheduler.getWorkStealingCount();
	}
}
