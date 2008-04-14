package se.sics.kompics.timer;

import se.sics.kompics.core.ComponentUUID;

/**
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
public class TimerId {

	private final ComponentUUID componentUUID;

	private final long id;

	public TimerId(ComponentUUID componentIdentifier, long id) {
		super();
		this.componentUUID = componentIdentifier;
		this.id = id;
	}

	public ComponentUUID getComponentIdentifier() {
		return componentUUID;
	}

	public long getId() {
		return id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((componentUUID == null) ? 0 : componentUUID.hashCode());
		result = prime * result + (int) (id ^ (id >>> 32));
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
		final TimerId other = (TimerId) obj;
		if (componentUUID == null) {
			if (other.componentUUID != null)
				return false;
		} else if (!componentUUID.equals(other.componentUUID))
			return false;
		if (id != other.id)
			return false;
		return true;
	}
}
