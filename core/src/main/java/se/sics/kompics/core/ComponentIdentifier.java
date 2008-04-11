package se.sics.kompics.core;

import java.util.UUID;

public class ComponentIdentifier {

	private static ThreadLocal<ComponentIdentifier> threadLocalComponentIdentifier = new ThreadLocal<ComponentIdentifier>();

	private UUID uuid;

	public ComponentIdentifier() {
		super();
		this.uuid = UUID.randomUUID();
	}

	public static ComponentIdentifier get() {
		return threadLocalComponentIdentifier.get();
	}

	public static void set(ComponentIdentifier componentIdentifier) {
		threadLocalComponentIdentifier.set(componentIdentifier);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ComponentIdentifier other = (ComponentIdentifier) obj;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}
}
