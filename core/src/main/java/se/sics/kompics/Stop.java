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
 * This lifecycle events instructs a component to stop.
 * 
 * @author Cosmin Arad {@literal <cosmin@sics.se>}
 * @author Jim Dowling {@literal <jdowling@sics.se>}
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public final class Stop implements KompicsEvent {

    /**
     * Singleton instance.
     * 
     * @deprecated since 1.2.0, use {@link #EVENT} instead.
     */
    @Deprecated
    public static final Stop event = new Stop();

    /**
     * Singleton instance.
     * 
     */
    public static final Stop EVENT = new Stop();

    /**
     * Instantiates a new stop event.
     * 
     * Developers should prefer to use {@link #EVENT} instead.
     */
    public Stop() {
    }
}
