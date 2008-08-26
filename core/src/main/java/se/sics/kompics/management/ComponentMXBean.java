package se.sics.kompics.management;

import java.util.SortedMap;

public interface ComponentMXBean {

	public long getPublishedEventCount();

	public long getHandledEventCount();

	public SortedMap<Long, String> getHandledEvents();

	public EventCounter[] getEventHandledCounters();

	public EventCounter[] getEventPublishedCounters();

	public String[] getPublishedEventCounters();

	public String[] getHandledEventCounters();

	public SortedMap<Long, String> getPublishedEvents();

	public ComponentMXBean getParent();

	public ComponentMXBean[] getChildren();

	public ChannelMXBean[] getChannels();

	public String getName();

	public ComponentEventCounter[] getCounters();
}
