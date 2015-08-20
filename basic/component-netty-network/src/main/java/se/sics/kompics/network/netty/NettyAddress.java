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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import se.sics.kompics.network.Address;

/**
 *
 * @author lkroll
 */
public class NettyAddress implements Address {

    private final InetSocketAddress isa;

    public NettyAddress(InetSocketAddress isa) {
        this.isa = isa;
    }

    public NettyAddress(Address other) {
        this(other.asSocket());
    }

    public NettyAddress(InetAddress addr, int port) {
        this(new InetSocketAddress(addr, port));
    }

    @Override
    public InetAddress getIp() {
        return this.isa.getAddress();
    }

    @Override
    public int getPort() {
        return this.isa.getPort();
    }

    @Override
    public InetSocketAddress asSocket() {
        return this.isa;
    }

    @Override
    public boolean sameHostAs(Address other) {
        return this.isa.equals(other.asSocket());
    }

    @Override
    public final String toString() {
        return isa.toString();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 11 * hash + (this.isa != null ? this.isa.hashCode() : 0);
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
        final NettyAddress other = (NettyAddress) obj;
        if (this.isa != other.isa && (this.isa == null || !this.isa.equals(other.isa))) {
            return false;
        }
        return true;
    }

}
