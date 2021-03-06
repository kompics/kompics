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

/**
 * The <code>Fault</code> class.
 * 
 * @author Cosmin Arad {@literal <cosmin@sics.se>}
 * @author Jim Dowling {@literal <jdowling@sics.se>}
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public class Fault implements KompicsEvent {

    private final Throwable cause;
    final ComponentCore source;
    private final KompicsEvent event;

    /**
     * Instantiates a new fault.
     * 
     * @param throwable
     *            the exception that caused the fault
     * @param source
     *            the component where the fault originated
     * @param event
     *            the event that caused the fault
     */
    public Fault(Throwable throwable, ComponentCore source, KompicsEvent event) {
        this.cause = throwable;
        this.source = source;
        this.event = event;
    }

    /**
     * Get the component where the fault originated.
     * 
     * @return the origin component
     */
    public ComponentDefinition getSource() {
        return source.getComponent();
    }

    /**
     * Get the runtime core instance of the component where the fault originated.
     * 
     * @return the origin runtime core
     */
    public Component getSourceCore() {
        return source;
    }

    /**
     * Get the exception that caused the fault.
     * 
     * @return the original exception
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * The event that was being handled when the fault occurred.
     * 
     * @return the original event
     */
    public KompicsEvent getEvent() {
        return this.event;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("KompicsFault(");
        sb.append(cause);
        sb.append(" thrown in ");
        sb.append(source);
        sb.append(" while handling event ");
        sb.append(event);
        sb.append(")");
        return sb.toString();
    }

    /**
     * The events that can be taken to resolve a fault.
     * 
     * See {@link ComponentDefinition#handleFault(Fault)} for semantics of the values.
     */
    public static enum ResolveAction {
        RESOLVED, IGNORE, DESTROY, ESCALATE;
    }
}
