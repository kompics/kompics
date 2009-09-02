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

import java.util.Comparator;

/**
 * The <code>PieceAvailability</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class PieceAvailability {

	private final int[] count;

	public PieceAvailability(int size) {
		count = new int[size];
	}

	public int getAvailability(int piece) {
		return count[piece];
	}

	public void addPiece(int piece) {
		count[piece]++;
	}

	public void removePiece(int piece) {
		if (count[piece] > 0) {
			count[piece]--;
		}
	}

	public void addPeer(Bitfield bitfield) {
		for (int i = 0; i < bitfield.getSize(); i++) {
			if (bitfield.get(i)) {
				count[i]++;
			}
		}
	}

	public void removePeer(Bitfield bitfield) {
		for (int i = 0; i < bitfield.getSize(); i++) {
			if (bitfield.get(i) && count[i] > 0) {
				count[i]--;
			}
		}
	}

	/**
	 * sorts pieces by their availability. Lowest available first.
	 */
	public Comparator<Integer> lowestFirst() {
		return new Comparator<Integer>() {
			@Override
			public int compare(Integer p1, Integer p2) {
				if (count[p1] < count[p2])
					return -1;
				if (count[p1] > count[p2])
					return 1;
				return 0;
			}
		};
	}
}
