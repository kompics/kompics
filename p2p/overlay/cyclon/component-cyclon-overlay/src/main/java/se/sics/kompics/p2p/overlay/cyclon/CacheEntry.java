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
package se.sics.kompics.p2p.overlay.cyclon;

import java.util.HashSet;

/**
 * The <code>CacheEntry</code> class represents an entry in a Cyclon node's
 * cache. It contains a node descriptor and it marks when and to what peer this
 * entry was last sent to. This information is used in the process of updating
 * the cache during a shuffle, so that the first cache entries removed are those
 * that were sent to the peer from whom we received the current shuffle
 * response.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class CacheEntry {

	/**
	 * the node descriptor held by this entry.
	 */
	private final CyclonNodeDescriptor descriptor;

	private final long addedAt;

	private long sentAt;

	/**
	 * remembers all peers that this node descriptor was sent to.
	 */
	private HashSet<CyclonAddress> sentTo;

	public CacheEntry(CyclonNodeDescriptor descriptor) {
		this.descriptor = descriptor;
		this.addedAt = System.currentTimeMillis();
		this.sentAt = 0;
		this.sentTo = null;
	}

	public boolean isEmpty() {
		return descriptor == null;
	}

	public void sentTo(CyclonAddress peer) {
		if (sentTo == null) {
			sentTo = new HashSet<CyclonAddress>();
		}
		sentTo.add(peer);
		sentAt = System.currentTimeMillis();
	}

	public CyclonNodeDescriptor getDescriptor() {
		return descriptor;
	}

	public long getAddedAt() {
		return addedAt;
	}

	public long getSentAt() {
		return sentAt;
	}

	public boolean wasSentTo(CyclonAddress peer) {
		return sentTo == null ? false : sentTo.contains(peer);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((descriptor == null) ? 0 : descriptor.hashCode());
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
		if (descriptor == null) {
			if (other.descriptor != null)
				return false;
		} else if (!descriptor.equals(other.descriptor))
			return false;
		return true;
	}
}
