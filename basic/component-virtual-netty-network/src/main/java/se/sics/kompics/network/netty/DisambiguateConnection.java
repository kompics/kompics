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
package se.sics.kompics.network.netty;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Transport;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class DisambiguateConnection implements Msg {

    public final Address src;
    public final Address dst;
    public final Transport protocol;
    public final int udtPort;
    public final boolean reply;

    public DisambiguateConnection(Address src, Address dst, Transport protocol, int udtPort, boolean reply) {
        this.src = src;
        this.dst = dst;
        this.protocol = protocol;
        this.udtPort = udtPort;
        this.reply = reply;
    }

    @Override
    public Address getSource() {
        return src;
    }

    @Override
    public Address getDestination() {
        return dst;
    }

    @Override
    public Address getOrigin() {
        return src;
    }

    @Override
    public Transport getProtocol() {
        return protocol;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName());
        sb.append("(");
        sb.append("SRC: ");
        sb.append(src);
        sb.append(", DST: ");
        sb.append(dst);
        sb.append(", PRT: ");
        sb.append(protocol);
        sb.append(", UDTport: ");
        sb.append(udtPort);
        sb.append(", reply? ");
        sb.append(reply);
        sb.append(")");
        return sb.toString();
    }
    
    

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (this.src != null ? this.src.hashCode() : 0);
        hash = 79 * hash + (this.dst != null ? this.dst.hashCode() : 0);
        hash = 79 * hash + (this.protocol != null ? this.protocol.hashCode() : 0);
        hash = 79 * hash + this.udtPort;
        hash = 79 * hash + (this.reply ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DisambiguateConnection other = (DisambiguateConnection) obj;
        if (this.src != other.src && (this.src == null || !this.src.equals(other.src))) {
            return false;
        }
        if (this.dst != other.dst && (this.dst == null || !this.dst.equals(other.dst))) {
            return false;
        }
        if (this.protocol != other.protocol) {
            return false;
        }
        if (this.udtPort != other.udtPort) {
            return false;
        }
        if (this.reply != other.reply) {
            return false;
        }
        return true;
    }
    
    
}
