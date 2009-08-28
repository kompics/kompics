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
package se.sics.kompics.p2p.overlay.chord;

import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.overlay.OverlayAddress;
import se.sics.kompics.p2p.overlay.key.NumericRingKey;

/**
 * The <code>ChordAddress</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class ChordAddress extends OverlayAddress implements
		Comparable<ChordAddress> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2007691693883454607L;

	private final NumericRingKey key;

	public ChordAddress(Address address, NumericRingKey key) {
		super(address);
		this.key = key;
	}

	public final NumericRingKey getKey() {
		return key;
	}

	@Override
	public int compareTo(ChordAddress that) {
		return key.compareTo(that.key);
	}

	@Override
	public String toString() {
		return key.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChordAddress other = (ChordAddress) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		return true;
	}

}
