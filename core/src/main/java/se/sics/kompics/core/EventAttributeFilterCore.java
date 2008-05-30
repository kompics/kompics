package se.sics.kompics.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import se.sics.kompics.api.Event;

public class EventAttributeFilterCore {

	private final Method attribute;

	private final Object value;

	public EventAttributeFilterCore(Method attribute, Object value) {
		super();
		this.attribute = attribute;
		this.value = value;
	}

	public Method getAttribute() {
		return attribute;
	}

	public Object getValue() {
		return value;
	}

	public boolean checkFilter(Event event) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		Object eventValue = attribute.invoke(event);
		return eventValue.equals(value);
	}
}
