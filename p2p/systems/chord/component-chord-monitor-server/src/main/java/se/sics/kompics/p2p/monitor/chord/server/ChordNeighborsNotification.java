/**
 * This file is part of the Kompics P2P Framework.
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
package se.sics.kompics.p2p.monitor.chord.server;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Transport;
import se.sics.kompics.p2p.overlay.chord.ChordNeighbors;

/**
 * The <code>ChordNeighborsNotification</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: ChordNeighborsNotification.java 1214 2009-09-06 14:36:29Z
 *          Cosmin $
 */
public final class ChordNeighborsNotification extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4823668150095672662L;

	private final Address peerAddress;

	private final int clientWebPort;

	private final ChordNeighbors chordNeighbors;

	public ChordNeighborsNotification(Address peerAddress, Address destination,
			Transport protocol, int clientWebPort, ChordNeighbors neighbors) {
		super(peerAddress, destination, protocol);
		this.peerAddress = peerAddress;
		this.chordNeighbors = neighbors;
		this.clientWebPort = clientWebPort;
	}

	public Address getPeerAddress() {
		return peerAddress;
	}

	public ChordNeighbors getChordNeighbors() {
		return chordNeighbors;
	}

	public int getClientWebPort() {
		return clientWebPort;
	}
}
