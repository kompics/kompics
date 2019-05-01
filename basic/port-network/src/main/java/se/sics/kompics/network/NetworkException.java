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
package se.sics.kompics.network;

import com.google.common.base.Optional;
import se.sics.kompics.KompicsEvent;

/**
 * The <code>NetworkException</code> class.
 * <p>
 * 
 * @author Cosmin Arad {@literal <cosmin@sics.se>}
 * @author Jim Dowling {@literal <jdowling@sics.se>}
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public final class NetworkException implements KompicsEvent {

    public final Address peer;
    public final Transport protocol;
    public final String message;
    public final Optional<Throwable> cause;

    public NetworkException(String message, Address peer, Transport protocol) {
        this.message = message;
        this.peer = peer;
        this.protocol = protocol;
        this.cause = Optional.absent();
    }

    public NetworkException(String message, Address peer, Transport protocol, Optional<Throwable> cause) {
        this.message = message;
        this.peer = peer;
        this.protocol = protocol;
        this.cause = cause;
    }

}
