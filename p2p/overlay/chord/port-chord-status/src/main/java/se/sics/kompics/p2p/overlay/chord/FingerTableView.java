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

import java.io.Serializable;

import se.sics.kompics.p2p.overlay.key.NumericRingKey;

/**
 * The <code>FingerTableView</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class FingerTableView implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3006468260052165096L;

	public final ChordAddress owner;

	public final NumericRingKey[] begin;

	public final NumericRingKey[] end;

	public final ChordAddress[] finger;

	public final long[] lastUpdated;

	public FingerTableView(ChordAddress ownerPeer, NumericRingKey[] begin,
			NumericRingKey[] end, ChordAddress[] fingers, long[] lastUpdated) {
		this.owner = ownerPeer;
		this.begin = begin;
		this.end = end;
		this.finger = fingers;
		this.lastUpdated = lastUpdated;
	}
}
