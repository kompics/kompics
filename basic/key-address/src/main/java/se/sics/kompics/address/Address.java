/**
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.kompics.address;

import com.google.common.base.Objects;
import com.google.common.primitives.Ints;
import com.google.common.primitives.UnsignedBytes;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Comparator;

/**
 * The
 * <code>Address</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id: Address.java 725 2009-03-08 14:05:05Z Cosmin $
 */
public final class Address implements Serializable, Comparable<Address> {

    /**
     *
     */
    private static final long serialVersionUID = -7330046056166039992L;
    private final InetAddress ip;
    private final int port;
    private final byte[] id;

    /**
     * Instantiates a new address.
     *
     * @param ip the ip
     * @param port the port
     * @param id the id
     */
    public Address(InetAddress ip, int port, byte[] id) {
        this.ip = ip;
        this.port = port;
        this.id = id;
    }
    
    public Address(InetAddress ip, int port, byte id) {
        this(ip, port, new byte[] {id});
    }

    /**
     * Gets the ip.
     *
     * @return the ip
     */
    public final InetAddress getIp() {
        return ip;
    }

    /**
     * Gets the port.
     *
     * @return the port
     */
    public final int getPort() {
        return port;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public final byte[] getId() {
        return id;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ip.getHostAddress());
        sb.append(':');
        sb.append(port);
        sb.append('/');

        IdUtils.printFormat(id, sb);

        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public final int hashCode() {
        final int prime = 31;
        int result;
        result = prime + id.hashCode();
        result = prime * result + ((ip == null) ? 0 : ip.hashCode());
        result = prime * result + port;
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Address other = (Address) obj;
        if (!Objects.equal(ip, other.ip)) {
            return false;
        }
        if (!Objects.equal(port, other.port)) {
            return false;
        }
        if (id == null) {
            if (other.id == null) {
                return true;
            }
            return false;
        }
        return Arrays.equals(id, other.id);
    }

    @Override
    public int compareTo(Address that) {
        ByteBuffer thisIpBytes = ByteBuffer.wrap(this.ip.getAddress()).order(
                ByteOrder.BIG_ENDIAN);
        ByteBuffer thatIpBytes = ByteBuffer.wrap(that.ip.getAddress()).order(
                ByteOrder.BIG_ENDIAN);

        int ipres = thisIpBytes.compareTo(thatIpBytes);
        if (ipres != 0) {
            return ipres;
        }

        if (this.port != that.port) {
            return this.port - that.port;
        }

        return byteLexComp.compare(id, that.id);
    }
    private static Comparator<byte[]> byteLexComp = UnsignedBytes.lexicographicalComparator();
    
    public Address newVirtual(byte[] id) throws UnknownHostException {
        return new Address(InetAddress.getByAddress(this.ip.getAddress()), this.port, id); //Should be safe to reuse InetAddress object
    }
    
    public Address newVirtual(byte id) throws UnknownHostException {
        return new Address(InetAddress.getByAddress(this.ip.getAddress()), this.port, id); //Should be safe to reuse InetAddress object
    }
    
    public boolean sameHostAs(Address other) {
        return ip.equals(other.ip) && (this.port == other.port);
    }
}
