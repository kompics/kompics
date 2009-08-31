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

import java.math.BigInteger;

import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.overlay.OverlayAddress;

/**
 * The <code>CyclonAddress</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class CyclonAddress extends OverlayAddress implements
		Comparable<CyclonAddress> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7582889514221620065L;

	private final BigInteger cyclonId;

	public CyclonAddress(Address address, BigInteger cyclonId) {
		super(address);
		this.cyclonId = cyclonId;
	}

	public BigInteger getCyclonId() {
		return cyclonId;
	}

	@Override
	public int compareTo(CyclonAddress that) {
		return cyclonId.compareTo(that.cyclonId);
	}

	@Override
	public String toString() {
		return cyclonId.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((cyclonId == null) ? 0 : cyclonId.hashCode());
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
		CyclonAddress other = (CyclonAddress) obj;
		if (cyclonId == null) {
			if (other.cyclonId != null)
				return false;
		} else if (!cyclonId.equals(other.cyclonId))
			return false;
		return true;
	}

}
