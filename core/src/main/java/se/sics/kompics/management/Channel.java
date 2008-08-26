package se.sics.kompics.management;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import se.sics.kompics.api.Event;
import se.sics.kompics.core.ChannelCore;

public class Channel implements ChannelMXBean {

	private ChannelCore core;

	private AtomicLong publishedEventCount;

	private Map<String, AtomicLong> publishedEvents;

	public Channel(ChannelCore core) {
		this.core = core;
		publishedEventCount = new AtomicLong(0);
		publishedEvents = new ConcurrentHashMap<String, AtomicLong>();
	}

	public long getPublishedEventCount() {
		return publishedEventCount.get();
	}

	public SortedMap<Long, String> getPublishedEvents() {
		TreeMap<Long, String> map = new TreeMap<Long, String>();
		for (Map.Entry<String, AtomicLong> entry : publishedEvents.entrySet()) {
			map.put(entry.getValue().get(), entry.getKey());
		}
		return map;
	}

	public EventCounter[] getEventPublishedCounters() {
		EventCounter[] array = new EventCounter[publishedEvents.size()];
		int i = 0;
		for (Map.Entry<String, AtomicLong> entry : publishedEvents.entrySet()) {
			if (i < array.length) {
				array[i++] = new EventCounter(entry.getValue().get(), entry
						.getKey());
			}
		}
		Arrays.sort(array);
		return array;
	}

	public String[] getPublishedEventCounters() {
		EventCounter[] array = getEventPublishedCounters();
		String[] strings = new String[array.length];
		for (int i = 0; i < strings.length; i++) {
			strings[i] = array[i].getEvent() + " " + array[i].getCount();
		}
		return strings;
	}

	public void publishedEvent(Event event) {
		publishedEventCount.incrementAndGet();
		AtomicLong counter = publishedEvents.get(event.getClass().getName());
		if (counter != null) {
			counter.incrementAndGet();
			return;
		}
		publishedEvents.put(event.getClass().getName(), new AtomicLong(1));
	}

	public String[] getEventTypes() {
		Set<Class<? extends Event>> types = core.getEventTypes();
		String[] names = new String[types.size()];
		int i = 0;
		for (Class<? extends Event> type : types) {
			names[i++] = type.getName();
		}
		return names;
	}
}
