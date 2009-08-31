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
package se.sics.kompics.p2p.overlay.chord.ring;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;

import se.sics.kompics.p2p.overlay.chord.ChordAddress;
import se.sics.kompics.p2p.overlay.key.RingKey.IntervalBounds;

/**
 * The <code>SuccessorList</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class SuccessorList implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5474640382521469058L;

	private final int length;

	private final ChordAddress self;

	private ArrayList<ChordAddress> successors;

	private final BigInteger ringSize;

	public SuccessorList(int length, ChordAddress self, BigInteger ringSize) {
		super();
		this.length = length;
		this.self = self;
		this.successors = new ArrayList<ChordAddress>(length + 1);
		this.successors.add(self);

		this.ringSize = ringSize;
	}

	public ChordAddress getSuccessor() {
		return successors.get(0);
	}

	public void setSuccessor(ChordAddress succ) {
		if (!successors.get(0).equals(succ)) {
			successors.add(0, succ);
			trimToLength();
			trimSelf();
		}
	}

	public void updateSuccessorList(SuccessorList list) {
		ArrayList<ChordAddress> myList = new ArrayList<ChordAddress>(successors);
		ArrayList<ChordAddress> theirList = list.getSuccessors();

		// we merge the 2 lists
		int m = 0, t = 0, f = 0;
		ChordAddress mine, theirs, last = list.getLocalPeer();

		successors.clear();

		successors.add(f++, last);

		while (f < length && m < myList.size() && t < theirList.size()) {
			mine = myList.get(m);
			theirs = theirList.get(t);

			if (mine == null) {
				m++;
				continue;
			}
			if (theirs == null) {
				t++;
				continue;
			}

			if (mine.getKey().belongsTo(last.getKey(), theirs.getKey(),
					IntervalBounds.OPEN_OPEN, ringSize)) {
				if (mine.getKey().belongsTo(last.getKey(), self.getKey(),
						IntervalBounds.OPEN_OPEN, ringSize)) {
					successors.add(f++, mine);
					last = mine;
				}
				m++;
			} else if (theirs.getKey().belongsTo(last.getKey(), mine.getKey(),
					IntervalBounds.OPEN_OPEN, ringSize)) {
				if (theirs.getKey().belongsTo(last.getKey(), self.getKey(),
						IntervalBounds.OPEN_OPEN, ringSize)) {
					successors.add(f++, theirs);
					last = theirs;
				}
				t++;
			} else {
				m++;
				t++;
			}
		}

		while (f < length && m < myList.size()) {
			mine = myList.get(m);
			if (mine == null) {
				m++;
				continue;
			}

			if (mine.getKey().belongsTo(last.getKey(), self.getKey(),
					IntervalBounds.OPEN_OPEN, ringSize)) {
				successors.add(f++, mine);
				last = mine;
			}
			m++;
		}

		while (f < length && t < theirList.size()) {
			theirs = theirList.get(t);
			if (theirs == null) {
				t++;
				continue;
			}

			if (theirs.getKey().belongsTo(last.getKey(), self.getKey(),
					IntervalBounds.OPEN_OPEN, ringSize)) {
				successors.add(f++, theirs);
				last = theirs;
			}
			t++;
		}

		while (successors.size() > f) {
			successors.remove(f);
		}
	}

	public void successorFailed(ChordAddress peer) {
		successors.remove(peer);

		if (successors.size() == 0) {
			// last successor died
			successors.add(self);
		}
	}

	public int getLength() {
		return length;
	}

	public ChordAddress getLocalPeer() {
		return self;
	}

	public ArrayList<ChordAddress> getSuccessors() {
		return successors;
	}

	public ArrayList<ChordAddress> getSuccessorListView() {
		return new ArrayList<ChordAddress>(successors);
	}

	private void trimToLength() {
		while (successors.size() > length) {
			successors.remove(length);
		}
	}

	private void trimSelf() {
		if (successors.size() > 1
				&& successors.get(successors.size() - 1).equals(self)) {
			successors.remove(successors.size() - 1);
		}
	}
}
