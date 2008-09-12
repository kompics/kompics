package se.sics.kompics.api;


public abstract class FastEventFilter<E extends Event> {

	protected final String attribute;

	protected final Object value;

	protected FastEventFilter(String attribute, Object value) {
		this.attribute = attribute;
		this.value = value;
	}

	public abstract boolean filter(E event);

	public final String getAttribute() {
		return attribute;
	}

	public final Object getValue() {
		return value;
	}
}
