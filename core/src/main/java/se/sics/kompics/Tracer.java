/*
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) 
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * This program is free software; you can redistribute it and/or
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
 * An interface for tracing code to be injected into the Kompics runtime.
 * 
 * Since these methods will be called for *every* event, slow implementations
 * case have *significant* performance impact for the runtime.
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public interface Tracer {
    /**
     * Gets called whenever a component triggers an event.
     * 
     * Always invoked on the triggering component's thread, and thus can access
     * core without additional synchronisation.
     * 
     * @param event the event that was triggered
     * @param port the port instance it was triggered on
     * @return true -> the event gets processed by the port, false -> the event gets discarded
     */
    public boolean triggeredOutgoing(KompicsEvent event, PortCore<?> port);
    
    /**
     * Gets called whenever a port receives an incoming event.
     * 
     * Always invoked on the triggering component's thread, and thus requires
     * additional synchronisation to access receiving component's core.
     * 
     * @param event the event that was triggered
     * @param port the port instance it was triggered on
     * @return true -> the event gets processed by the port, false -> the event gets discarded
     */
    public boolean triggeredIncoming(KompicsEvent event, PortCore<?> port);
}
