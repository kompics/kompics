package se.sics.kompics.core.scheduler;

import se.sics.kompics.core.ChannelCore;
import se.sics.kompics.core.EventCore;
import se.sics.kompics.core.EventHandlerCore;

public class Work {

	public static int SIZE = 1000000000;

	static final ThreadLocal<FreeList> freeList = new ThreadLocal<FreeList>() {
		@Override
		protected FreeList initialValue() {
			return new FreeList(null, 0);
		}
	};

	public static class FreeList {
		Work head;
		int size;
		public int foundEmpty;
		public int foundFull;

		public FreeList(Work head, int size) {
			this.head = head;
			this.size = size;
		}
	}

	public static Work acquire(ChannelCore channelCore, EventCore eventCore,
			EventHandlerCore eventHandlerCore) {
		FreeList free = freeList.get();
		Work w = free.head;
		if (w == null) {
			free.foundEmpty++;
			return new Work(channelCore, eventCore, eventHandlerCore);
		}
		// recycle existing node
		free.head = w.next;
		free.size--;
		// initialize
		w.recycle(channelCore, eventCore, eventHandlerCore);
		return w;
	}

	public static void free(Work work) {
		FreeList free = freeList.get();
		if (free.size < SIZE) {
			work.next = free.head;
			work.eventCore = null;
			free.head = work;
			free.size++;
			return;
		}
		free.foundFull++;
	}

	public static int[] getStats() {
		FreeList free = freeList.get();
		int ret[] = new int[2];
		ret[0] = free.foundEmpty;
		ret[1] = free.foundFull;
		return ret;
	}

	// Work proper starts here
	
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
		this.next = null;
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
