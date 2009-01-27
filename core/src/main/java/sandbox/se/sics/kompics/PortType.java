package sandbox.se.sics.kompics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public abstract class PortType {

	private static HashMap<Class<? extends PortType>, PortType> map = new HashMap<Class<? extends PortType>, PortType>();

	private Set<Class<? extends Event>> positive = new HashSet<Class<? extends Event>>();
	private Set<Class<? extends Event>> negative = new HashSet<Class<? extends Event>>();

	Class<? extends PortType> portTypeClass;

	@SuppressWarnings("unchecked")
	public static <P extends PortType> P getPortType(Class<P> portTypeClass) {
		P portType = (P) map.get(portTypeClass);
		if (portType == null) {
			try {
				portType = portTypeClass.newInstance();
				map.put(portTypeClass, portType);
			} catch (InstantiationException e) {
				throw new RuntimeException("Cannot create port type "
						+ portTypeClass.getCanonicalName(), e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException("Cannot create port type "
						+ portTypeClass.getCanonicalName(), e);
			}
		}
		return portType;
	}

	protected void positive(Class<? extends Event> eventType) {
		positive.add(eventType);
	}

	protected void negative(Class<? extends Event> eventType) {
		negative.add(eventType);
	}

	public boolean hasPositive(Class<? extends Event> eventType) {
		if (positive.contains(eventType)) {
			return true;
		}
		for (Class<? extends Event> eType : positive) {
			if (eType.isAssignableFrom(eventType)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasNegative(Class<? extends Event> eventType) {
		if (negative.contains(eventType)) {
			return true;
		}
		for (Class<? extends Event> eType : negative) {
			if (eType.isAssignableFrom(eventType)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasEvent(boolean positive, Class<? extends Event> eventType) {
		return (positive == true ? hasPositive(eventType)
				: hasNegative(eventType));
	}

	@Override
	public String toString() {
		return getClass().getCanonicalName() + " = Positive: "
				+ positive.toString() + ", Negative: " + negative.toString();
	}
}
