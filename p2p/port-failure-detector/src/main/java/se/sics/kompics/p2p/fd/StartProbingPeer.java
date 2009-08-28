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
package se.sics.kompics.p2p.fd;

import java.util.UUID;

import se.sics.kompics.Request;
import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.overlay.OverlayAddress;

/**
 * The <code>StartProbingPeer</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class StartProbingPeer extends Request {

	private final Address peerAddress;

	private final OverlayAddress overlayAddress;

	private final UUID requestId;

	public StartProbingPeer(Address peerAddress, OverlayAddress overlayAddress) {
		this.peerAddress = peerAddress;
		this.requestId = UUID.randomUUID();
		this.overlayAddress = overlayAddress;
	}

	public final Address getPeerAddress() {
		return peerAddress;
	}

	public final UUID getRequestId() {
		return requestId;
	}
	
	public OverlayAddress getOverlayAddress() {
		return overlayAddress;
	}
}
