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

/**
 * The <code>CyclonNodeDescriptor</code> class represent a Cyclon node
 * descriptor, containing a Cyclon address and an age.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class CyclonNodeDescriptor implements Comparable<CyclonNodeDescriptor> {

	private final CyclonAddress cyclonAddress;
	private int age;

	public CyclonNodeDescriptor(CyclonAddress cyclonAddress) {
		this.cyclonAddress = cyclonAddress;
		this.age = 0;
	}

	public int incrementAndGetAge() {
		age++;
		return age;
	}

	public int getAge() {
		return age;
	}

	public CyclonAddress getCyclonAddress() {
		return cyclonAddress;
	}

	@Override
	public int compareTo(CyclonNodeDescriptor that) {
		if (this.age > that.age)
			return 1;
		if (this.age < that.age)
			return -1;
		return 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((cyclonAddress == null) ? 0 : cyclonAddress.hashCode());
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
		CyclonNodeDescriptor other = (CyclonNodeDescriptor) obj;
		if (cyclonAddress == null) {
			if (other.cyclonAddress != null)
				return false;
		} else if (!cyclonAddress.equals(other.cyclonAddress))
			return false;
		return true;
	}
}
