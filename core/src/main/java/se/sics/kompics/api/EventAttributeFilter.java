package se.sics.kompics.api;

public class EventAttributeFilter {

	private final String attribute;

	private final Object value;

	public EventAttributeFilter(String attribute, Object value) {
		super();
		this.attribute = attribute;
		this.value = value;
	}

	public String getAttribute() {
		return attribute;
	}

	public Object getValue() {
		return value;
	}
}
