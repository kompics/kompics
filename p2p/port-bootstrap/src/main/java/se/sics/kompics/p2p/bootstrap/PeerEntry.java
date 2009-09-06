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
package se.sics.kompics.p2p.bootstrap;

import java.io.Serializable;

import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.overlay.OverlayAddress;

/**
 * The <code>PeerEntry</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class PeerEntry implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2424114253292450165L;

	private final String overlay;

	private final OverlayAddress overlayAddress;

	private final int peerWebPort;

	private final long age;

	private final long freshness;

	public PeerEntry(String overlay, OverlayAddress overlayAddress,
			int peerWebPort, Address address, long age, long freshness) {
		this.overlay = overlay;
		this.overlayAddress = overlayAddress;
		this.age = age;
		this.freshness = freshness;
		this.peerWebPort = peerWebPort;
	}

	public long getAge() {
		return age;
	}

	public long getFreshness() {
		return freshness;
	}

	public String getOverlay() {
		return overlay;
	}

	public OverlayAddress getOverlayAddress() {
		return overlayAddress;
	}
	
	public int getPeerWebPort() {
		return peerWebPort;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((overlay == null) ? 0 : overlay.hashCode());
		result = prime * result
				+ ((overlayAddress == null) ? 0 : overlayAddress.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PeerEntry other = (PeerEntry) obj;
		if (overlay == null) {
			if (other.overlay != null)
				return false;
		} else if (!overlay.equals(other.overlay))
			return false;
		if (overlayAddress == null) {
			if (other.overlayAddress != null)
				return false;
		} else if (!overlayAddress.equals(other.overlayAddress))
			return false;
		return true;
	}
}
