/* 
 * This file is part of the CaracalDB distributed storage system.
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
package se.sics.kompics.network.netty;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Transport;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public abstract class DisambiguateConnection {
    public static class Req extends Message {
        public final int localPort;
        public final int udtPort;
        
        public Req(Address src, Address dst, Transport protocol, int localPort, int udtPort) {
            super(src, dst, protocol);
            this.localPort = localPort;
            this.udtPort = udtPort;
        }
    }
    public static class Resp extends Message {
        public final int localPort;
        public final int boundPort;
        public final int udtPort;
        
        public Resp(Address src, Address dst, Transport protocol, int remotePort, int boundPort, int udtPort) {
            super(src, dst, protocol);
            this.localPort = remotePort; // rename because on the sender side it's remote but for the receiver it's local
            this.boundPort = boundPort;
            this.udtPort = udtPort;
        }
    }
}
