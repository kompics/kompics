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

import java.util.UUID;

/**
 * The <code>Component</code> class.
 * 
 * @author Cosmin Arad {@literal <cosmin@sics.se>}
 * @author Jim Dowling {@literal <jdowling@sics.se>}
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 * @version $Id$
 */
public interface Component extends Runnable {

    /**
     * Gets a positive (provided) instance of the given port.
     * 
     * @param <P>
     *            the type of the port type
     * @param portType
     *            the port type class instance
     * 
     * @return a positive instance of the port type
     */
    public <P extends PortType> Positive<P> getPositive(Class<P> portType);

    /**
     * Gets a provided (positive) instance of the given port.
     * 
     * Same as {@link #getPositive(Class)}.
     * 
     * @param <P>
     *            the type of the port type
     * @param portType
     *            the port type class instance
     * @return a provided instance of the port type
     */
    public <P extends PortType> Positive<P> provided(Class<P> portType);

    /**
     * Gets a negative (required) instance of the given port.
     * 
     * @param <P>
     *            the type of the port type
     * @param portType
     *            the port type class instance
     * 
     * @return a negative instance of the port type
     */
    public <P extends PortType> Negative<P> getNegative(Class<P> portType);

    /**
     * Gets a required (negative) instance of the given port.
     * 
     * Same as {@link #getNegative(Class)}.
     * 
     * @param <P>
     *            the type of the port type
     * @param portType
     *            the port type class instance
     * @return a required instance of the port type
     */
    public <P extends PortType> Negative<P> required(Class<P> portType);

    /**
     * Gets the control.
     * 
     * @return the control port
     */
    public Positive<ControlPort> getControl();

    /**
     * @return the component's control port.
     */
    public Positive<ControlPort> control();

    public ComponentDefinition getComponent();

    public void escalateFault(Fault fault);

    public UUID id();

    public State state();

    public static enum State {
        PASSIVE, STARTING, ACTIVE, STOPPING, FAULTY, DESTROYED;
    }
}
