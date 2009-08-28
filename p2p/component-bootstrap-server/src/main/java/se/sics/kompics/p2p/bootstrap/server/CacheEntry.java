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
package se.sics.kompics.p2p.bootstrap.server;

import java.util.UUID;

import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.overlay.OverlayAddress;

/**
 * The <code>CacheEntry</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class CacheEntry implements Comparable<CacheEntry> {

	private final Address peerAddress;

	private final String overlay;

	private final OverlayAddress overlayAddress;

	private long refreshedAt;

	private UUID evictionTimerId;

	private final long addedAt;

	public CacheEntry(Address peerAddress, String overlay,
			OverlayAddress overlayAddress, long now, long addedAt) {
		this.peerAddress = peerAddress;
		this.overlay = overlay;
		this.overlayAddress = overlayAddress;
		this.refreshedAt = now;
		this.addedAt = addedAt;
	}

	public Address getPeerAddress() {
		return peerAddress;
	}

	public String getOverlay() {
		return overlay;
	}

	public OverlayAddress getOverlayAddress() {
		return overlayAddress;
	}

	public long getRefreshedAt() {
		return refreshedAt;
	}

	public void setRefreshedAt(long refreshedAt) {
		this.refreshedAt = refreshedAt;
	}

	public long getAddedAt() {
		return addedAt;
	}

	public UUID getEvictionTimerId() {
		return evictionTimerId;
	}

	public void setEvictionTimerId(UUID evictionTimerId) {
		this.evictionTimerId = evictionTimerId;
	}

	@Override
	public int compareTo(CacheEntry that) {
		// more recent entries are lower than older entries
		if (this.refreshedAt > that.refreshedAt)
			return -1;
		if (this.refreshedAt < that.refreshedAt)
			return 1;
		return 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((overlay == null) ? 0 : overlay.hashCode());
		result = prime * result
				+ ((overlayAddress == null) ? 0 : overlayAddress.hashCode());
		result = prime * result
				+ ((peerAddress == null) ? 0 : peerAddress.hashCode());
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
		CacheEntry other = (CacheEntry) obj;
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
		if (peerAddress == null) {
			if (other.peerAddress != null)
				return false;
		} else if (!peerAddress.equals(other.peerAddress))
			return false;
		return true;
	}
}
