package se.sics.kompics.core;

import java.util.UUID;

public class ComponentUUID {

	private static ThreadLocal<ComponentUUID> threadLocalComponentIdentifier = new ThreadLocal<ComponentUUID>();

	private final UUID uuid;

	public ComponentUUID() {
		super();
		this.uuid = UUID.randomUUID();
	}

	static ComponentUUID get() {
		return threadLocalComponentIdentifier.get();
	}

	static void set(ComponentUUID componentIdentifier) {
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
		final ComponentUUID other = (ComponentUUID) obj;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}
}
