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
package se.sics.kompics.p2p.cdn.bittorrent.client;

import java.util.BitSet;
import java.util.Random;

/**
 * The <code>Bitfield</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class Bitfield {

	private final BitSet pieces;

	private final int size;

	public Bitfield(int size) {
		this.size = size;
		pieces = new BitSet(size);
	}

	public Bitfield(int size, Random r, double ratio) {
		this.size = size;
		if (ratio < 0 || ratio > 1) {
			throw new RuntimeException("ratio must be between 0 and 1");
		}
		pieces = new BitSet(size);
		for (int i = 0; i < size; i++) {
			pieces.set(i, r.nextDouble() < ratio);
		}
	}

	public Bitfield(Bitfield bitfield) {
		this.size = bitfield.size;
		pieces = (BitSet) bitfield.pieces.clone();
	}

	public boolean allSet() {
		return pieces.cardinality() == size;
	}

	public boolean has(int piece) {
		return pieces.get(piece);
	}

	public void set(int piece) {
		pieces.set(piece);
	}

	public void reset(int piece) {
		pieces.set(piece, false);
	}

	public void setAll(Bitfield bitfield) {
		pieces.or(bitfield.pieces);
	}

	public int getSize() {
		return size;
	}

	public int cardinality() {
		return pieces.cardinality();
	}

	public boolean isEmpty() {
		return pieces.isEmpty();
	}

	public Bitfield copy() {
		return new Bitfield(this);
	}

	public void and(Bitfield bf) {
		pieces.and(bf.pieces);
	}

	public void andNot(Bitfield bf) {
		pieces.andNot(bf.pieces);
	}

	public boolean get(int bitIndex) {
		return pieces.get(bitIndex);
	}

	public int length() {
		return pieces.length();
	}

	public int nextSetBit(int fromIndex) {
		return pieces.nextSetBit(fromIndex);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < size; i++) {
			sb.append(pieces.get(i) ? 1 : 0);
		}
		return sb.toString();
	}
}
