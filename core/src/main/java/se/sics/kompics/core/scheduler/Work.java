package se.sics.kompics.core.scheduler;

import java.util.concurrent.ConcurrentLinkedQueue;

import se.sics.kompics.core.ChannelCore;
import se.sics.kompics.core.EventCore;
import se.sics.kompics.core.EventHandler;

public class Work {

	private static ConcurrentLinkedQueue<Work> free = new ConcurrentLinkedQueue<Work>();

	public static Work aquire(ChannelCore channelCore, EventCore eventCore,
			EventHandler eventHandler) {
		Work work = free.poll();
		if (work != null) {
			work.recycle(channelCore, eventCore, eventHandler);
			return work;
		} else {
			return new Work(channelCore, eventCore, eventHandler);
		}
	}

	public static void release(Work work) {
		free.add(work);
	}

	private ChannelCore channelCore;

	private EventCore eventCore;

	private EventHandler eventHandler;

	public Work(ChannelCore channelCore, EventCore eventCore,
			EventHandler eventHandler) {
		this.channelCore = channelCore;
		this.eventCore = eventCore;
		this.eventHandler = eventHandler;
	}

	public void recycle(ChannelCore channelCore, EventCore eventCore,
			EventHandler eventHandler) {
		this.channelCore = channelCore;
		this.eventCore = eventCore;
		this.eventHandler = eventHandler;
	}

	public ChannelCore getChannelCore() {
		return channelCore;
	}

	public EventCore getEventCore() {
		return eventCore;
	}

	public EventHandler getEventHandler() {
		return eventHandler;
	}
}
