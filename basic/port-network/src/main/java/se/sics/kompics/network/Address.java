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

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 *
 * @author lkroll
 */
public interface Address {
    /**
     * 
     * @return the IP address part of this object
     */
    public InetAddress getIp();
    /**
     * 
     * @return the port part of this object
     */
    public int getPort();
    
    /**
     * Get this address as InetSocketAddress.
     * 
     * This is used for lookups within network implementation, so it better be fast.
     * Preferably no new object creation should happen as part of this call.
     * 
     * @return ip+port of this address.
     */
    public InetSocketAddress asSocket();
    
    /**
     * Compares only the ip+port part of the address for equality.
     * 
     * This is used to decide whether or not to reflect messages back up without serialising.
     * 
     * Most likely the same as "this.asSocket().equals(other.asSocket())".
     * 
     * @param other
     * @return 
     */
    public boolean sameHostAs(Address other);
}
