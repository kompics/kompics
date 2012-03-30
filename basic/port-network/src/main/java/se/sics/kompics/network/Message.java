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

import java.io.Serializable;

import se.sics.kompics.Event;
import se.sics.kompics.address.Address;

/**
 * The <code>Message</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
public abstract class Message extends Event implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2644373757327105586L;

	private final Address source;

	private Address destination;
	
	private transient Transport protocol;

	private transient boolean highPriority;
	
	/**
	 * Instantiates a new message.
	 * 
	 * @param source
	 *            the source
	 * @param destination
	 *            the destination
	 */
	protected Message(Address source, Address destination) {
		this(source, destination, Transport.TCP);
	}

	/**
	 * Instantiates a new message.
	 * 
	 * @param source
	 *            the source
	 * @param destination
	 *            the destination
	 * @param protocol
	 *            the protocol
	 */
	protected Message(Address source, Address destination, Transport protocol) {
		this(source, destination, protocol, true);
	}

	/**
	 * Instantiates a new message.
	 * 
	 * @param source the source
	 * @param destination the destination
	 * @param protocol the protocol
	 * @param highPriority whether this message is should be sent with high priority 
	 */
	protected Message(Address source, Address destination, Transport protocol,
			boolean highPriority) {
		this.source = source;
		this.destination = destination;
		this.protocol = protocol;
		this.highPriority = highPriority;
	}
	
	/**
	 * Gets the source.
	 * 
	 * @return the source
	 */
	public final Address getSource() {
		return source;
	}

	/**
	 * Gets the destination.
	 * 
	 * @return the destination
	 */
	public final Address getDestination() {
		return destination;
	}
	
	public void setDestination(Address destination) {
		this.destination = destination;
	}
	
	/**
	 * Sets the protocol.
	 * 
	 * @param protocol
	 *            the new protocol
	 */
	public final void setProtocol(Transport protocol) {
		this.protocol = protocol;
	}
	
	/**
	 * Gets the protocol.
	 * 
	 * @return the protocol
	 */
	public final Transport getProtocol() {
		return protocol;
	}
	
	/**
	 * @return whether this is a high priority message (default)
	 */
	public final boolean isHighPriority() {
		return highPriority;
	}
}
