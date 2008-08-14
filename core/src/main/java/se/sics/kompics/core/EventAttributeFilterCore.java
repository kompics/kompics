package se.sics.kompics.core;

import java.lang.reflect.Field;

import se.sics.kompics.api.Event;

public class EventAttributeFilterCore {

	private final Field attribute;

	private final Object value;

	public EventAttributeFilterCore(Field attribute, Object value) {
		super();
		this.attribute = attribute;
		this.value = value;
	}

	public Field getAttribute() {
		return attribute;
	}

	public Object getValue() {
		return value;
	}

	public boolean checkFilter(Event event) throws IllegalArgumentException,
			IllegalAccessException {
		Object eventValue = attribute.get(event);
		return eventValue.equals(value);
	}
}
