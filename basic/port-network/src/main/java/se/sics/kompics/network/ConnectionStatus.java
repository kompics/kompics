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
package se.sics.kompics.network;

import se.sics.kompics.KompicsEvent;

/**
 *  All subclasses indicate a status change of a connection to the specified {@code peer}.
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public abstract class ConnectionStatus implements KompicsEvent {

    /**
     * The address of the node the connection relates to.
     */
    public final Address peer;
    /**
     * The transport protocol of the connection to the {@code peer}.
     */
    public final Transport protocol;

    private ConnectionStatus(Address peer, Transport protocol) {
        this.peer = peer;
        this.protocol = protocol;
    }

    public static Requested requested(Address peer, Transport protocol) {
        return new Requested(peer, protocol);
    }

    public static Established established(Address peer, Transport protocol) {
        return new Established(peer, protocol);
    }

    public static Dropped dropped(Address peer, Transport protocol, boolean last) {
        return new Dropped(peer, protocol, last);
    }

    /**
     * Indicates that a connection to the {@code peer} was requested, for example by sending a message to it.
     *
     */
    public static class Requested extends ConnectionStatus {
        private Requested(Address peer, Transport protocol) {
            super(peer, protocol);
        }
    }

    /**
     * Indicates that a connection to the {@code peer} has been established.
     *
     */
    public static class Established extends ConnectionStatus {
        private Established(Address peer, Transport protocol) {
            super(peer, protocol);
        }
    }

    /**
     * Indicates that a connection to the {@code peer} was dropped.
     *
     */
    public static class Dropped extends ConnectionStatus {

        /**
         * Was the last channel to the {@code peer} dropped?
         * 
         * If {@code true}, this is definitely equivalent to a session loss event.
         * 
         * If {@code false}, it may just be the case that a duplicate channel was closed, which does not incur message
         * losses, and should thus not be treated as a session loss event.
         * 
         */
        public final boolean last;

        private Dropped(Address peer, Transport protocol, boolean last) {
            super(peer, protocol);
            this.last = last;
        }
    }
}
