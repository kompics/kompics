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

/**
 * The <code>StringRingKey</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class StringRingKey implements RingKey {

	private final String id;

	public StringRingKey(String id) {
		this.id = id;
	}

	@Override
	public final boolean belongsTo(RingKey from, RingKey to,
			IntervalBounds bounds) {
		return belongsTo((StringRingKey) from, (StringRingKey) to, bounds);
	}

	public final boolean belongsTo(StringRingKey from, StringRingKey to,
			IntervalBounds bounds) {
		/*
		 * we have 3 cases: 1) the interval definitely does not wrap around
		 * MAX_STRING; 2) the interval definitely wraps around MAX_STRING; 3)
		 * special case: from == to, it so depends on the bounds
		 */
		if (from.id.compareTo(to.id) < 0) {
			// 1) from < to (interval does not wrap around MAX_STRING)
			if (bounds.equals(IntervalBounds.OPEN_OPEN)) {
				return (id.compareTo(from.id) > 0) && (id.compareTo(to.id) < 0);
			} else if (bounds.equals(IntervalBounds.OPEN_CLOSED)) {
				return (id.compareTo(from.id) > 0)
						&& (id.compareTo(to.id) <= 0);
			} else if (bounds.equals(IntervalBounds.CLOSED_OPEN)) {
				return (id.compareTo(from.id) >= 0)
						&& (id.compareTo(to.id) < 0);
			} else if (bounds.equals(IntervalBounds.CLOSED_CLOSED)) {
				return (id.compareTo(from.id) >= 0)
						&& (id.compareTo(to.id) <= 0);
			} else {
				throw new RuntimeException("Unknown interval bounds");
			}
		} else if (from.id.compareTo(to.id) > 0) {
			/*
			 * 2) from > to (interval wraps around MAX_STRING): here we do the
			 * same as in 1) on the reverse interval (which does not wrap)
			 */
			if (bounds.equals(IntervalBounds.OPEN_OPEN)) {
				return !((id.compareTo(from.id) <= 0) && (id.compareTo(to.id) >= 0));
			} else if (bounds.equals(IntervalBounds.OPEN_CLOSED)) {
				return !((id.compareTo(from.id) <= 0) && (id.compareTo(to.id) > 0));
			} else if (bounds.equals(IntervalBounds.CLOSED_OPEN)) {
				return !((id.compareTo(from.id) < 0) && (id.compareTo(to.id) >= 0));
			} else if (bounds.equals(IntervalBounds.CLOSED_CLOSED)) {
				return !((id.compareTo(from.id) < 0) && (id.compareTo(to.id) > 0));
			} else {
				throw new RuntimeException("Unknown interval bounds");
			}
		} else {
			// 3) from == to
			if (bounds.equals(IntervalBounds.OPEN_OPEN)) {
				// id belongs to interval only if not equal to from
				return !id.equals(from.id);
			} else if (bounds.equals(IntervalBounds.OPEN_CLOSED)) {
				// the interval the covers whole ring
				return true;
			} else if (bounds.equals(IntervalBounds.CLOSED_OPEN)) {
				// the interval the covers whole ring
				return true;
			} else if (bounds.equals(IntervalBounds.CLOSED_CLOSED)) {
				// id belongs to interval only if not equal to from
				return id.equals(from.id);
			} else {
				throw new RuntimeException("Unknown interval bounds");
			}
		}
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
		StringRingKey other = (StringRingKey) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
