package se.sics.kompics.management;

import java.util.SortedMap;

public interface KompicsMXBean {

	public int getWorkerCount();

	public void setWorkerCount(int workerCount);

	public long getPublishedEventCount();

	public long getHandledEventCount();

	public SortedMap<Long, String> getHandledEvents();

	public EventCounter[] getEventHandledCounters();

	public EventCounter[] getEventPublishedCounters();

	public String[] getPublishedEventCounters();

	public String[] getHandledEventCounters();

	public SortedMap<Long, String> getPublishedEvents();
}
