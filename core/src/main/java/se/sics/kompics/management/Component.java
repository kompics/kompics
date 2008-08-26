package se.sics.kompics.management;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import se.sics.kompics.api.Event;
import se.sics.kompics.core.ChannelCore;
import se.sics.kompics.core.ComponentCore;

public class Component implements ComponentMXBean {

	private ComponentCore core;

	private String name;

	private AtomicLong publishedEventCount;

	private AtomicLong handledEventCount;

	private Map<String, AtomicLong> publishedEvents;

	private Map<String, AtomicLong> handledEvents;

	public Component(ComponentCore core, String name) {
		this.core = core;
		this.name = name;
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

	public ComponentEventCounter[] getCounters() {
		Set<String> events = new HashSet<String>(handledEvents.keySet());
		events.addAll(publishedEvents.keySet());
		ComponentEventCounter[] counters = new ComponentEventCounter[events
				.size()];
		int i = 0;
		for (String event : events) {
			long published = publishedEvents.get(event) == null ? 0
					: publishedEvents.get(event).get();
			long handled = handledEvents.get(event) == null ? 0 : handledEvents
					.get(event).get();
			counters[i++] = new ComponentEventCounter(published, handled, event);
		}
		return counters;
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

	public ComponentMXBean[] getChildren() {
		LinkedList<ComponentCore> childrenCores = core.getSubComponentCores();
		ComponentMXBean[] childrenBeans = new ComponentMXBean[childrenCores
				.size()];
		int i = 0;
		for (ComponentCore componentCore : childrenCores) {
			childrenBeans[i++] = componentCore.mbean;
		}
		return childrenBeans;
	}

	public ChannelMXBean[] getChannels() {
		LinkedList<ChannelCore> channelCores = core.getLocalChannelCores();
		ChannelMXBean[] channelBeans = new ChannelMXBean[channelCores.size()];
		int i = 0;
		for (ChannelCore channelCore : channelCores) {
			channelBeans[i++] = channelCore.mbean;
		}
		return channelBeans;
	}

	public ComponentMXBean getParent() {
		ComponentCore parentCore = core.getParentCore();
		return parentCore == null ? null : parentCore.mbean;
	}

	public String getName() {
		return name;
	}
}
