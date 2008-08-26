package se.sics.kompics.management;

import java.util.SortedMap;

public interface ChannelMXBean {

	public long getPublishedEventCount();

	public EventCounter[] getEventPublishedCounters();

	public String[] getPublishedEventCounters();

	public SortedMap<Long, String> getPublishedEvents();

	public String[] getEventTypes();
}
