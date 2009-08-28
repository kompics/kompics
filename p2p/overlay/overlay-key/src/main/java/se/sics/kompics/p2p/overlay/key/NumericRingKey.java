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
package se.sics.kompics.p2p.overlay.key;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * The <code>NumericRingKey</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class NumericRingKey implements RingKey, Serializable,
		Comparable<NumericRingKey> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3472289897908435858L;

	public static final BigInteger RING_SIZE = BigInteger.valueOf(2).pow(160);

	private final BigInteger id;

	public NumericRingKey(BigInteger id) {
		this.id = id;
	}

	public NumericRingKey(long id) {
		this.id = BigInteger.valueOf(id);
	}

	public final BigInteger getId() {
		return id;
	}

	@Override
	public final boolean belongsTo(RingKey from, RingKey to,
			IntervalBounds bounds) {
		return belongsTo((NumericRingKey) from, (NumericRingKey) to, bounds);
	}

	public final boolean belongsTo(NumericRingKey from, NumericRingKey to,
			IntervalBounds bounds) {
		return belongsTo(from, to, bounds, RING_SIZE);
	}

	public final boolean belongsTo(NumericRingKey from, NumericRingKey to,
			IntervalBounds bounds, BigInteger ringSize) {
		BigInteger ny = modMinus(to.id, from.id, ringSize);
		BigInteger nx = modMinus(id, from.id, ringSize);

		if (bounds.equals(IntervalBounds.OPEN_OPEN)) {
			return ((from.id.equals(to.id) && !id.equals(from.id)) || (nx
					.compareTo(BigInteger.ZERO) > 0 && nx.compareTo(ny) < 0));
		} else if (bounds.equals(IntervalBounds.OPEN_CLOSED)) {
			return (from.id.equals(to.id) || (nx.compareTo(BigInteger.ZERO) > 0 && nx
					.compareTo(ny) <= 0));
		} else if (bounds.equals(IntervalBounds.CLOSED_OPEN)) {
			return (from.id.equals(to.id) || (nx.compareTo(BigInteger.ZERO) >= 0 && nx
					.compareTo(ny) < 0));
		} else if (bounds.equals(IntervalBounds.CLOSED_CLOSED)) {
			return ((from.id.equals(to.id) && id.equals(from.id)) || (nx
					.compareTo(BigInteger.ZERO) >= 0 && nx.compareTo(ny) <= 0));
		} else {
			throw new RuntimeException("Unknown interval bounds");
		}
	}

	public final NumericRingKey successor(BigInteger ringSize) {
		return new NumericRingKey(modPlus(id, BigInteger.ONE, ringSize));
	}

	public final NumericRingKey next() {
		return new NumericRingKey(id.add(BigInteger.ONE));
	}

	public final BigInteger ringPlus(NumericRingKey add, BigInteger ringSize) {
		return modPlus(id, add.id, ringSize);
	}

	public final BigInteger ringMinus(NumericRingKey subtract,
			BigInteger ringSize) {
		return modMinus(id, subtract.id, ringSize);
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NumericRingKey other = (NumericRingKey) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return id.toString();
	}

	private static BigInteger modMinus(BigInteger x, BigInteger y,
			BigInteger ringSize) {
		return ringSize.add(x).subtract(y).mod(ringSize);
	}

	private static BigInteger modPlus(BigInteger x, BigInteger y,
			BigInteger ringSize) {
		return x.add(y).mod(ringSize);
	}

	/**
	 * Beware that two ring keys are essentially not comparable. This method is
	 * provided for convenience.
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(NumericRingKey that) {
		return this.id.compareTo(that.id);
	}
}
