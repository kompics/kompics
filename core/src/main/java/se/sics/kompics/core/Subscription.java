package se.sics.kompics.core;

import java.lang.reflect.Field;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.EventFilter;

public class Subscription {

	private final ComponentReference componentReference;

	private final ChannelReference channelReference;

	private final EventHandlerCore eventHandlerCore;

	// private final EventAttributeFilterCore[] filters;

	private final EventFilter<Event> eventFilter;
	private final Field field;
//	private final FastEventFilter<? extends Event> eventFilter;

	public Subscription(ComponentReference componentReference,
			ChannelReference channelReference,
			EventHandlerCore eventHandlerCore,
//			EventAttributeFilterCore[] filters) {
		EventFilter<Event> eventFilter, Field field) {
		this.componentReference = componentReference;
		this.channelReference = channelReference;
		this.eventHandlerCore = eventHandlerCore;
		this.eventFilter = (EventFilter<Event>) eventFilter;
		this.field = field;
	}

	public ComponentReference getComponent() {
		return componentReference;
	}

	public ChannelReference getChannel() {
		return channelReference;
	}

	public EventHandlerCore getEventHandlerCore() {
		return eventHandlerCore;
	}

	public EventFilter<Event> getEventFilter() {
		return eventFilter;
	}

	public Field getField() {
		return field;
	}

//	public EventAttributeFilterCore[] getFilters() {
//		return filters;
//	}
}
