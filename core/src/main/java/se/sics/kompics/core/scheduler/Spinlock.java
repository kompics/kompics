package se.sics.kompics.core.scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

public class Spinlock {

	private AtomicBoolean free = new AtomicBoolean(true);

	public void lock() {
		while (true) {
			while (!free.get()) {};
			if (free.compareAndSet(true, false))
				return;
		}
	}

	public void unlock() {
		free.set(true);
	}
}
