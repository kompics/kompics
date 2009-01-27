package sandbox.se.sics.kompics;

import java.util.concurrent.atomic.AtomicBoolean;

public final class Spinlock {

	private final AtomicBoolean free = new AtomicBoolean(true);

	public final void lock() {
		while (true) {
			while (!free.get())
				;
			if (free.compareAndSet(true, false))
				return;
		}
	}

	public final void unlock() {
		free.set(true);
	}
}
