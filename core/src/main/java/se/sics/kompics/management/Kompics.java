package se.sics.kompics.management;

import java.util.Arrays;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import se.sics.kompics.api.Event;

public class Kompics implements KompicsMXBean {

	private se.sics.kompics.api.Kompics kompics;

	private AtomicLong publishedEventCount;

	private AtomicLong handledEventCount;

	private Map<String, AtomicLong> publishedEvents;

	private Map<String, AtomicLong> handledEvents;

	public Kompics(se.sics.kompics.api.Kompics kompics) {
		this.kompics = kompics;
		publishedEventCount = new AtomicLong(0);
		handledEventCount = new AtomicLong(0);
		publishedEvents = new ConcurrentHashMap<String, AtomicLong>();
		handledEvents = new ConcurrentHashMap<String, AtomicLong>();
	}

	public long getHandledEventCount() {
		return handledEventCount.get();
	}

	public long getPublishedEventCount() {
		return publishedEventCount.get();
	}

	public int getWorkerCount() {
		return kompics.getWorkerCount();
	}

	public void setWorkerCount(int workerCount) {
		kompics.setWorkerCount(workerCount);
	}

	public SortedMap<Long, String> getPublishedEvents() {
		TreeMap<Long, String> map = new TreeMap<Long, String>();
		for (Map.Entry<String, AtomicLong> entry : publishedEvents.entrySet()) {
			map.put(entry.getValue().get(), entry.getKey());
		}
		return map;
	}

	public SortedMap<Long, String> getHandledEvents() {
		TreeMap<Long, String> map = new TreeMap<Long, String>();
		for (Map.Entry<String, AtomicLong> entry : handledEvents.entrySet()) {
			map.put(entry.getValue().get(), entry.getKey());
		}
		return map;
	}

	public EventCounter[] getEventHandledCounters() {
		EventCounter[] array = new EventCounter[handledEvents.size()];
		int i = 0;
		for (Map.Entry<String, AtomicLong> entry : handledEvents.entrySet()) {
			if (i < array.length) {
				array[i++] = new EventCounter(entry.getValue().get(), entry
						.getKey());
			}
		}
		Arrays.sort(array);
		return array;
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

	public String[] getHandledEventCounters() {
		EventCounter[] array = getEventHandledCounters();
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

	public void handledEvent(Event event) {
		handledEventCount.incrementAndGet();
		AtomicLong counter = handledEvents.get(event.getClass().getName());
		if (counter != null) {
			counter.incrementAndGet();
			return;
		}
		handledEvents.put(event.getClass().getName(), new AtomicLong(1));
	}
}
