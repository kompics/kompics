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
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public abstract class ConnectionStatus implements KompicsEvent {

    public final Address peer;
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

    public static class Requested extends ConnectionStatus {
        private Requested(Address peer, Transport protocol) {
            super(peer, protocol);
        }
    }

    public static class Established extends ConnectionStatus {
        private Established(Address peer, Transport protocol) {
            super(peer, protocol);
        }
    }

    public static class Dropped extends ConnectionStatus {

        public final boolean last;

        private Dropped(Address peer, Transport protocol, boolean last) {
            super(peer, protocol);
            this.last = last;
        }
    }
}
