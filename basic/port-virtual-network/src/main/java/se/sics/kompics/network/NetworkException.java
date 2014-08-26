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
package se.sics.kompics.network;

import java.net.InetSocketAddress;

import se.sics.kompics.Event;

/**
 * The <code>NetworkException</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id: NetworkException.java 636 2009-02-08 01:41:23Z Cosmin $
 */
public final class NetworkException extends Event {

	private final InetSocketAddress remoteAddress;
	private final Transport protocol;

	/**
	 * Instantiates a new network exception.
	 * 
	 * @param remoteAddress
	 *            the remote address
	 */
	public NetworkException(InetSocketAddress remoteAddress, Transport protocol) {
		super();
		this.remoteAddress = remoteAddress;
		this.protocol = protocol;
	}

	/**
	 * Gets the remote address.
	 * 
	 * @return the remote address
	 */
	public final InetSocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	/**
	 * Gets the protocol
	 * 
	 * @return the protocol
	 */
	public final Transport getProtocol() {
		return protocol;
	}
}
