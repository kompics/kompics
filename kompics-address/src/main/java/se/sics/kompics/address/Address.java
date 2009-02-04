/**
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
package se.sics.kompics.address;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * The <code>Address</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
public final class Address implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7330046056166039991L;

	private final InetAddress ip;

	private final int port;

	private final int id;

	/**
	 * Instantiates a new address.
	 * 
	 * @param ip
	 *            the ip
	 * @param port
	 *            the port
	 * @param id
	 *            the id
	 */
	public Address(InetAddress ip, int port, int id) {
		this.ip = ip;
		this.port = port;
		this.id = id;
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
	public final int getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return "" + id + ip + ":" + port;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((ip == null) ? 0 : ip.hashCode());
		result = prime * result + port;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Address other = (Address) obj;
		if (id != other.id)
			return false;
		if (ip == null) {
			if (other.ip != null)
				return false;
		} else if (!ip.equals(other.ip))
			return false;
		if (port != other.port)
			return false;
		return true;
	}
}
