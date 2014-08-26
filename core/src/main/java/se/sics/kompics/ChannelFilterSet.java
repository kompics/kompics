package se.sics.kompics;

import java.util.ArrayList;
import java.util.HashMap;

public class ChannelFilterSet {

	private HashMap<Class<? extends KompicsEvent>, ArrayList<Class<? extends ChannelFilter<?, ?>>>> filterTypesByEventType;
	private HashMap<Class<? extends ChannelFilter<?, ?>>, ArrayList<ChannelFilter<?, ?>>> filtersByFilterType;
	private HashMap<Class<? extends ChannelFilter<?, ?>>, HashMap<Object, ArrayList<ChannelCore<?>>>> channelsByFilterType;

	// for removal
	private HashMap<ChannelCore<?>, ChannelFilter<?, ?>> filtersByChannel;
	private HashMap<Class<? extends KompicsEvent>, ArrayList<Class<? extends KompicsEvent>>> inheritedFilters;

	public ChannelFilterSet() {
		filterTypesByEventType = new HashMap<Class<? extends KompicsEvent>, ArrayList<Class<? extends ChannelFilter<?, ?>>>>();
		filtersByFilterType = new HashMap<Class<? extends ChannelFilter<?, ?>>, ArrayList<ChannelFilter<?, ?>>>();
		channelsByFilterType = new HashMap<Class<? extends ChannelFilter<?, ?>>, HashMap<Object, ArrayList<ChannelCore<?>>>>();

		filtersByChannel = new HashMap<ChannelCore<?>, ChannelFilter<?, ?>>();
		inheritedFilters = new HashMap<Class<? extends KompicsEvent>, ArrayList<Class<? extends KompicsEvent>>>();
	}

	// public boolean containsChannel(ChannelCore<?> channel) {
	// return filtersByChannel.containsKey(channel);
	// }

	@SuppressWarnings("unchecked")
	public void addChannelFilter(ChannelCore<?> channel, ChannelFilter<?, ?> filter) {
		Class<? extends KompicsEvent> eventType = filter.getEventType();
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

		// keep it in filtersByChannel for removal
		filtersByChannel.put(channel, filter);
	}

	@SuppressWarnings("unchecked")
	public void removeChannel(ChannelCore<?> channel) {
		ChannelFilter<?, ?> filter = filtersByChannel.get(channel);
		if (filter == null) {
			// not a filtered channel
			return;
		}

		filtersByChannel.remove(channel);

		// undo add
		Class<? extends KompicsEvent> eventType = filter.getEventType();
		Class<? extends ChannelFilter<?, ?>> filterType = (Class<? extends ChannelFilter<?, ?>>) filter
				.getClass();

		ArrayList<Class<? extends ChannelFilter<?, ?>>> filterTypes = filterTypesByEventType
				.get(eventType);

		// remove filter
		ArrayList<ChannelFilter<?, ?>> filters = filtersByFilterType
				.get(filterType);
		filters.remove(filter);
		if (filters.size() == 0) {
			filtersByFilterType.remove(filterType);
		}

		// remove channel
		HashMap<Object, ArrayList<ChannelCore<?>>> channelsByValue = channelsByFilterType
				.get(filterType);
		ArrayList<ChannelCore<?>> channels = channelsByValue.get(filter
				.getValue());

		channels.remove(channel);
		if (channels.isEmpty()) {
			channelsByValue.remove(filter.getValue());
			if (channelsByValue.isEmpty()) {
				channelsByFilterType.remove(filterType);
				filterTypes.remove(filterType);
				if (filterTypes.isEmpty()) {
					filterTypesByEventType.remove(eventType);

					ArrayList<Class<? extends KompicsEvent>> inheritants = inheritedFilters
							.get(eventType);
					if (inheritants != null) {
						for (Class<? extends KompicsEvent> eType : inheritants) {
							filterTypesByEventType.remove(eType);
						}
						inheritedFilters.remove(eventType);
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public ArrayList<ChannelCore<?>> get(KompicsEvent event) {
		ArrayList<ChannelCore<?>> result = new ArrayList<ChannelCore<?>>();
		Class<? extends KompicsEvent> eventType = event.getClass();
		ArrayList<Class<? extends ChannelFilter<?, ?>>> filterTypes = filterTypesByEventType
				.get(eventType);

		if (filterTypes == null) {
			// no filter types found for this event type. we try to add this
			// event type since it may be a sub-type of a filtered event type.
			for (Class<? extends KompicsEvent> eType : filterTypesByEventType.keySet()) {
				if (eType.isAssignableFrom(eventType)) {
					// I have a filter for a super-type, so I copy the filter
					// structure for the super-type to this event type
					filterTypes = new ArrayList<Class<? extends ChannelFilter<?, ?>>>(
							filterTypesByEventType.get(eType));
					filterTypesByEventType.put(eventType, filterTypes);

					ArrayList<Class<? extends KompicsEvent>> inheritants = inheritedFilters
							.get(eType);
					if (inheritants == null) {
						inheritants = new ArrayList<Class<? extends KompicsEvent>>();
						inheritedFilters.put(eType, inheritants);
					}
					inheritants.add(eventType);
					break;
				}
			}
		}

		if (filterTypes != null) {
			// filterTypes may still be null for unfiltered event types
			for (int i = 0; i < filterTypes.size(); i++) {
				// for each type of filter
				ChannelFilter<?, ?> f = filtersByFilterType.get(
						filterTypes.get(i)).get(0);
				Object attValue = ((ChannelFilter<KompicsEvent, ?>) f).getValue(event);

				HashMap<Object, ArrayList<ChannelCore<?>>> channelsByValue = channelsByFilterType
						.get(filterTypes.get(i));

				ArrayList<ChannelCore<?>> chans = channelsByValue.get(attValue);
				if (chans != null) {
					result.addAll(chans);
				}
			}
		}

		// if (result.size() > 0)
		// System.err.println("SIZE= " + result.size() + " FOR " + event);
		if (result.size() > 1) {
			return distinct(result);
		}
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

	public boolean isEmpty() {
		return filtersByChannel.isEmpty();
	}
}
