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
package se.sics.kompics.p2p.cdn.bittorrent.address;

import java.math.BigInteger;

import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.overlay.OverlayAddress;

/**
 * The <code>BitTorrentAddress</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: BitTorrentAddress.java 1072 2009-08-28 09:03:02Z Cosmin $
 */
public final class BitTorrentAddress extends OverlayAddress implements
		Comparable<BitTorrentAddress> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -2750050431431113987L;

	private final BigInteger peerId;

	public BitTorrentAddress(Address peerAddress, BigInteger peerId) {
		super(peerAddress);
		this.peerId = peerId;
	}

	public BigInteger getPeerId() {
		return peerId;
	}

	@Override
	public int compareTo(BitTorrentAddress that) {
		return this.peerId.compareTo(that.peerId);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((peerId == null) ? 0 : peerId.hashCode());
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
		BitTorrentAddress other = (BitTorrentAddress) obj;
		if (peerId == null) {
			if (other.peerId != null)
				return false;
		} else if (!peerId.equals(other.peerId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return peerId.toString();
	}
}
