package se.sics.kompics.core.scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

public class TASSpinlock {

	private AtomicBoolean free = new AtomicBoolean(true);

	public void lock() {
		while (!free.compareAndSet(true, false))
			;
	}

	public void unlock() {
		free.set(true);
	}
}
