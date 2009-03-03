package se.sics.kompics;

import java.util.ArrayList;
import java.util.HashMap;

public class ChannelFilterSet {

	private HashMap<Class<? extends Event>, ArrayList<Class<? extends ChannelFilter<?, ?>>>> filterTypesByEventType;

	private HashMap<Class<? extends ChannelFilter<?, ?>>, ArrayList<ChannelFilter<?, ?>>> filtersByFilterType;

	private HashMap<Class<? extends ChannelFilter<?, ?>>, HashMap<Object, ArrayList<ChannelCore<?>>>> channelsByFilterType;

	public ChannelFilterSet() {
		filterTypesByEventType = new HashMap<Class<? extends Event>, ArrayList<Class<? extends ChannelFilter<?, ?>>>>();
		filtersByFilterType = new HashMap<Class<? extends ChannelFilter<?, ?>>, ArrayList<ChannelFilter<?, ?>>>();
		channelsByFilterType = new HashMap<Class<? extends ChannelFilter<?, ?>>, HashMap<Object, ArrayList<ChannelCore<?>>>>();
	}

	@SuppressWarnings("unchecked")
	void addChannelFilter(ChannelCore<?> channel, ChannelFilter<?, ?> filter) {
		Class<? extends Event> eventType = filter.getEventType();
		Class<? extends ChannelFilter<?, ?>> filterType = (Class<? extends ChannelFilter<?, ?>>) filter
				.getClass();

		// add filter type if not already there
		ArrayList<Class<? extends ChannelFilter<?, ?>>> filterTypes = filterTypesByEventType
				.get(eventType);
		if (filterTypes == null) {
			filterTypes = new ArrayList<Class<? extends ChannelFilter<?, ?>>>();
			filterTypesByEventType.put(eventType, filterTypes);
		}
		if (!filterTypes.contains(filterType)) {
			System.err.println("ADDED " + filter.getValue() + filter.getClass());
			
			filterTypes.add(filterType);
		}

		// add filter
		ArrayList<ChannelFilter<?, ?>> filters = filtersByFilterType
				.get(filterType);
		if (filters == null) {
			filters = new ArrayList<ChannelFilter<?, ?>>();
			filtersByFilterType.put(filterType, filters);
		}
		filters.add(filter);

		// add filter value
		HashMap<Object, ArrayList<ChannelCore<?>>> channelsByValue = channelsByFilterType
				.get(filterType);
		if (channelsByValue == null) {
			channelsByValue = new HashMap<Object, ArrayList<ChannelCore<?>>>();
			channelsByFilterType.put(filterType, channelsByValue);
		}
		// add channel
		ArrayList<ChannelCore<?>> channels = channelsByValue.get(filter
				.getValue());
		if (channels == null) {
			channels = new ArrayList<ChannelCore<?>>();
			channelsByValue.put(filter.getValue(), channels);
		}
		channels.add(channel);
	}

	@SuppressWarnings("unchecked")
	ArrayList<ChannelCore<?>> get(Event event) {
		ArrayList<ChannelCore<?>> result = new ArrayList<ChannelCore<?>>();
		ArrayList<Class<? extends ChannelFilter<?, ?>>> filterTypes = filterTypesByEventType
				.get(event.getClass());
		if (filterTypes != null) {
			for (int i = 0; i < filterTypes.size(); i++) {
				// for each type of filter
				ChannelFilter<?, ?> f = filtersByFilterType.get(
						filterTypes.get(i)).get(0);
				Object attValue = ((ChannelFilter<Event, ?>) f).getValue(event);

				HashMap<Object, ArrayList<ChannelCore<?>>> channelsByValue = channelsByFilterType
						.get(filterTypes.get(i));
				result.addAll(channelsByValue.get(attValue));
			}
			System.err.println("SIZE= " + result.size() + " FOR " + event);
		}
		if (result.size() > 1)
			return distinct(result);
		return result;
	}

	private ArrayList<ChannelCore<?>> distinct(ArrayList<ChannelCore<?>> array) {
		ArrayList<ChannelCore<?>> result = new ArrayList<ChannelCore<?>>();
		for (int i = 0; i < array.size(); i++) {
			ChannelCore<?> c = array.get(i);
			if (!result.contains(c)) {
				result.add(c);
			}
		}
		return result;
	}
}
