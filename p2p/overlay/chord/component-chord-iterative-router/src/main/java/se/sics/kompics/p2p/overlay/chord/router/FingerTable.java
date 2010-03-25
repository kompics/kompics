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
package se.sics.kompics.p2p.overlay.chord.router;

import java.math.BigInteger;

import se.sics.kompics.p2p.overlay.chord.ChordAddress;
import se.sics.kompics.p2p.overlay.chord.FingerTableView;
import se.sics.kompics.p2p.overlay.key.NumericRingKey;
import se.sics.kompics.p2p.overlay.key.RingKey.IntervalBounds;

/**
 * The <code>FingerTable</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class FingerTable {

	private int log2RingSize;

	private BigInteger ringSize;

	private ChordAddress localPeer;

	private NumericRingKey[] begin;

	private NumericRingKey[] end;

	private ChordAddress[] finger;

	private long[] lastUpdated;

	private ChordIterativeRouter router;

	private int nextFingerToFix;

	public FingerTable(int log2RingSize, ChordAddress localPeer,
			ChordIterativeRouter router) {
		this.router = router;

		this.log2RingSize = log2RingSize;
		ringSize = new BigInteger("2").pow(log2RingSize);
		this.localPeer = localPeer;

		begin = new NumericRingKey[log2RingSize];
		end = new NumericRingKey[log2RingSize];
		finger = new ChordAddress[log2RingSize];
		lastUpdated = new long[log2RingSize];

		nextFingerToFix = -1;

		initFingerTable();
	}

	private void initFingerTable() {
		long now = System.currentTimeMillis();
		for (int i = 0; i < log2RingSize; i++) {
			begin[i] = new NumericRingKey(new BigInteger("2").pow(i).add(
					localPeer.getKey().getId()).mod(ringSize));
			end[i] = new NumericRingKey(new BigInteger("2").pow(i + 1).add(
					localPeer.getKey().getId()).mod(ringSize));
			finger[i] = null;
			lastUpdated[i] = now;
		}
	}

	int nextFingerToFix() {
		nextFingerToFix++;
		if (nextFingerToFix >= log2RingSize) {
			nextFingerToFix = 0;
		}
		return nextFingerToFix;
	}

	void fingerFixed(int f, ChordAddress fingerPeer) {
		lastUpdated[f] = System.currentTimeMillis();
		finger[f] = null;
		if (fingerPeer == null || fingerPeer.equals(localPeer)) {
			return;
		}
		if (fingerPeer.getKey().belongsTo(begin[f], end[f],
				IntervalBounds.CLOSED_OPEN, ringSize)) {
			finger[f] = fingerPeer;
		}
		router.fingerTableChanged();
	}

	boolean learnedAboutFreshPeer(ChordAddress newPeer) {
		return learnedAboutPeer(newPeer, true, true);
	}

	boolean learnedAboutPeer(ChordAddress newPeer) {
		return learnedAboutPeer(newPeer, true, false);
	}

	boolean learnedAboutPeer(ChordAddress newPeer, boolean update, boolean fresh) {
		// we have learned about this new peer so we check whether it is a
		// better alternative for one of our fingers
		if (newPeer == null || newPeer.equals(localPeer)) {
			return false;
		}

		BigInteger distance = newPeer.getKey().ringMinus(localPeer.getKey(),
				ringSize);
		int i = distance.bitLength() - 1;
		if (i == -1) {
			return false;
		}
		
		boolean changed = false;

		if (newPeer.getKey().belongsTo(begin[i], end[i],
				IntervalBounds.CLOSED_OPEN, ringSize)) {
			// it belongs to this interval
			// we update no matter what
			if (finger[i] == null
					|| (fresh
							&& newPeer.getKey().belongsTo(begin[i],
									finger[i].getKey(),
									IntervalBounds.CLOSED_OPEN, ringSize) && !finger[i]
							.getKey().equals(begin[i]))) {
				// it is closer to the beginning of the interval than
				// the current finger
				finger[i] = newPeer;
				lastUpdated[i] = System.currentTimeMillis();
				changed = true;
			}
		}

		if (changed && update) {
			router.fingerTableChanged();
		}
		return changed;
	}

	void fingerSuspected(ChordAddress peer) {
		boolean changed = false;

		long now = System.currentTimeMillis();

		for (int i = 0; i < finger.length; i++) {
			if (finger[i] != null && finger[i].equals(peer)) {
				finger[i] = null;
				lastUpdated[i] = now;
				changed = true;
			}
		}

		if (changed) {
			router.fingerTableChanged();
		}
	}

	ChordAddress closestPreceedingPeer(NumericRingKey key) {
		for (int i = log2RingSize - 1; i >= 0; i--) {
			if (finger[i] != null
					&& finger[i].getKey().belongsTo(localPeer.getKey(), key,
							IntervalBounds.OPEN_OPEN, ringSize))
				return finger[i];
		}
		return localPeer;
	}

	FingerTableView getView() {
		FingerTableView view = new FingerTableView(localPeer, begin.clone(),
				end.clone(), finger.clone(), lastUpdated.clone());
		return view;
	}

	public NumericRingKey getFingerBegin(int fingerIndex) {
		return begin[fingerIndex];
	}
}
