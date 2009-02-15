/**
 * This file is part of the Kompics component model runtime.
 * 
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

// TODO: Auto-generated Javadoc
/**
 * The <code>PortType</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
public abstract class PortType {

	private static HashMap<Class<? extends PortType>, PortType> map = new HashMap<Class<? extends PortType>, PortType>();

	private Set<Class<? extends Event>> positive = new HashSet<Class<? extends Event>>();
	private Set<Class<? extends Event>> negative = new HashSet<Class<? extends Event>>();

	Class<? extends PortType> portTypeClass;

	/**
	 * Gets the port type.
	 * 
	 * @param portTypeClass
	 *            the port type class
	 * 
	 * @return the port type
	 */
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

	/**
	 * Positive.
	 * 
	 * @param eventType
	 *            the event type
	 */
	protected final void positive(Class<? extends Event> eventType) {
		positive.add(eventType);
	}

	/**
	 * Negative.
	 * 
	 * @param eventType
	 *            the event type
	 */
	protected final void negative(Class<? extends Event> eventType) {
		negative.add(eventType);
	}

	/**
	 * Checks for positive.
	 * 
	 * @param eventType
	 *            the event type
	 * 
	 * @return true, if successful
	 */
	public final boolean hasPositive(Class<? extends Event> eventType) {
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

	/**
	 * Checks for negative.
	 * 
	 * @param eventType
	 *            the event type
	 * 
	 * @return true, if successful
	 */
	public final boolean hasNegative(Class<? extends Event> eventType) {
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

	/**
	 * Checks for event.
	 * 
	 * @param positive
	 *            the positive
	 * @param eventType
	 *            the event type
	 * 
	 * @return true, if successful
	 */
	public final boolean hasEvent(boolean positive, Class<? extends Event> eventType) {
		return (positive == true ? hasPositive(eventType)
				: hasNegative(eventType));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return getClass().getCanonicalName() + " = Positive: "
				+ positive.toString() + ", Negative: " + negative.toString();
	}
}
