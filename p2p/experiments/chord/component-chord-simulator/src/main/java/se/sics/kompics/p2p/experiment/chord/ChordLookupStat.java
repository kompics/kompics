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
package se.sics.kompics.p2p.experiment.chord;

import se.sics.kompics.p2p.overlay.key.NumericRingKey;

/**
 * The <code>ChordLookupStat</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class ChordLookupStat {

	private final boolean successful;
	private final boolean correct;
	private final NumericRingKey initiator;
	private final NumericRingKey key;
	private final NumericRingKey responsible;
	private final long duration;
	private final int hops;

	public ChordLookupStat(boolean successful, boolean correct,
			NumericRingKey initiator, NumericRingKey key,
			NumericRingKey responsible, long duration, int hops) {
		super();
		this.successful = successful;
		this.correct = correct;
		this.initiator = initiator;
		this.key = key;
		this.responsible = responsible;
		this.duration = duration;
		this.hops = hops;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public boolean isCorrect() {
		return correct;
	}

	public NumericRingKey getInitiator() {
		return initiator;
	}

	public NumericRingKey getKey() {
		return key;
	}

	public NumericRingKey getResponsible() {
		return responsible;
	}

	public long getDuration() {
		return duration;
	}

	public int getHops() {
		return hops;
	}
}
