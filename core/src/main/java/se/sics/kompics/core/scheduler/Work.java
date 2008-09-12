package se.sics.kompics.core.scheduler;

import java.util.concurrent.atomic.AtomicInteger;

import se.sics.kompics.core.ChannelCore;
import se.sics.kompics.core.EventCore;
import se.sics.kompics.core.EventHandlerCore;

public class Work {
	private static ThreadLocal<Pool> threadLocalPool = new ThreadLocal<Pool>() {
		@Override
		protected Pool initialValue() {
			return new Pool(Thread.currentThread().getName());
		}
	};

	public static class Pool {
		private volatile Work freeWork;

		public AtomicInteger size = new AtomicInteger(0);

		private final String name;

		// private volatile int i, j;

		public Pool(String name) {
			this.name = name;
		}

		public static Pool get() {
			return threadLocalPool.get();
		}

		// public static void set(Pool pool) {
		// threadLocalPool.set(pool);
		// }

		public Work acquireWork(ChannelCore channelCore, EventCore eventCore,
				EventHandlerCore eventHandlerCore) {
			// i++;
			// if (i == 1000000) {
			// i = 0;
			// System.out.println("Acquired 1000000 from " + name);
			// }
			if (freeWork != null) {
				Work w = freeWork;
				freeWork = freeWork.next;
				size.decrementAndGet();
				w.recycle(channelCore, eventCore, eventHandlerCore);
				return w;
			} else {
				return new Work(channelCore, eventCore, eventHandlerCore);
			}
		}

		// we stack the released work so we reuse the most recently used work
		public void releaseWork(Work w) {
			// j++;
			// if (j == 1000000) {
			// j = 0;
			// System.out.println("Released 1000000 from " + name);
			// }
			w.next = freeWork;
			freeWork = w;
			size.incrementAndGet();
		}

	}

	public static Work acquire(ChannelCore channelCore, EventCore eventCore,
			EventHandlerCore eventHandlerCore) {
		Pool pool = Pool.get();
		if (pool != null) {
			return pool.acquireWork(channelCore, eventCore, eventHandlerCore);
		} else {
			return new Work(channelCore, eventCore, eventHandlerCore);
		}
	}

	public static void release(Work work) {
		Pool pool = Pool.get();
		if (pool != null) {
			pool.releaseWork(work);
		} else {
			System.err.println("OOOOOPS " + Thread.currentThread().getName());
		}
	}

	private ChannelCore channelCore;

	private EventCore eventCore;

	private EventHandlerCore eventHandlerCore;

	private Work next;

	public Work(ChannelCore channelCore, EventCore eventCore,
			EventHandlerCore eventHandlerCore) {
		this.channelCore = channelCore;
		this.eventCore = eventCore;
		this.eventHandlerCore = eventHandlerCore;
	}

	public void recycle(ChannelCore channelCore, EventCore eventCore,
			EventHandlerCore eventHandlerCore) {
		this.channelCore = channelCore;
		this.eventCore = eventCore;
		this.eventHandlerCore = eventHandlerCore;
		// System.out.println(".");
	}

	public ChannelCore getChannelCore() {
		return channelCore;
	}

	public EventCore getEventCore() {
		return eventCore;
	}

	public EventHandlerCore getEventHandlerCore() {
		return eventHandlerCore;
	}
}
