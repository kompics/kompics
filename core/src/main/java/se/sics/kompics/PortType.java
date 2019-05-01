/*
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

/**
 * The <code>PortType</code> class.
 * 
 * @author Cosmin Arad {@literal <cosmin@sics.se>}
 * @author Jim Dowling {@literal <jdowling@sics.se>}
 * @version $Id$
 */
public abstract class PortType {

    private static HashMap<Class<? extends PortType>, PortType> map = new HashMap<Class<? extends PortType>, PortType>();

    private Set<Class<? extends KompicsEvent>> positive = new HashSet<Class<? extends KompicsEvent>>();
    private Set<Class<? extends KompicsEvent>> negative = new HashSet<Class<? extends KompicsEvent>>();

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
                throw new RuntimeException("Cannot create port type " + portTypeClass.getCanonicalName(), e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot create port type " + portTypeClass.getCanonicalName(), e);
            }
        }
        return portType;
    }

    public static void preloadInstance(PortType p) {
        map.put(p.getClass(), p);
    }

    /**
     * specifies an indication, response, or confirmation event type
     * 
     * @param eventType
     *            the event type
     */
    protected final void positive(Class<? extends KompicsEvent> eventType) {
        positive.add(eventType);
    }

    /**
     * specifies an indication, response, or confirmation event type
     * 
     * @param eventType
     */
    protected final void indication(Class<? extends KompicsEvent> eventType) {
        positive.add(eventType);
    }

    /**
     * specifies a request event type
     * 
     * @param eventType
     *            the event type
     */
    protected final void negative(Class<? extends KompicsEvent> eventType) {
        negative.add(eventType);
    }

    /**
     * specifies a request event type
     * 
     * @param eventType
     */
    protected final void request(Class<? extends KompicsEvent> eventType) {
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
    public final boolean hasPositive(Class<? extends KompicsEvent> eventType) {
        if (positive.contains(eventType)) {
            return true;
        }
        for (Class<? extends KompicsEvent> eType : positive) {
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
    public final boolean hasNegative(Class<? extends KompicsEvent> eventType) {
        if (negative.contains(eventType)) {
            return true;
        }
        for (Class<? extends KompicsEvent> eType : negative) {
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
    public final boolean hasEvent(boolean positive, Class<? extends KompicsEvent> eventType) {
        return (positive == true ? hasPositive(eventType) : hasNegative(eventType));
    }

    Set<Class<? extends KompicsEvent>> getPositiveEvents() {
        return positive;
    }

    Set<Class<? extends KompicsEvent>> getNegativeEvents() {
        return negative;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public final String toString() {
        return getClass().getCanonicalName() + " = Positive: " + positive.toString() + ", Negative: "
                + negative.toString();
    }
}
