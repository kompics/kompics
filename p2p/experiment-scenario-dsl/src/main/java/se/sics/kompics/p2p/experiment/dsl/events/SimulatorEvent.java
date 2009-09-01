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
package se.sics.kompics.p2p.experiment.dsl.events;

import java.io.Serializable;

/**
 * The <code>SimulatorEvent</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class SimulatorEvent implements Comparable<SimulatorEvent>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7702413908499807140L;

	private static long sequence = 0;

	private long seqNo;

	private long time;

	private boolean onList = false;

	public SimulatorEvent(long time) {
		super();
		this.seqNo = sequence++;
		this.time = time;
	}

	public final long getTime() {
		return time;
	}

	protected void setTime(long time) {
		if (onList) {
			throw new RuntimeException(
					"Cannot change the time of a scheduled event");
		}
		this.time = time;
	}

	public final void setOnList(boolean on) {
		onList = on;
	}

	public final boolean isOnList() {
		return onList;
	}

	@Override
	public int compareTo(SimulatorEvent that) {
		if (this.time < that.time)
			return -1;
		if (this.time > that.time)
			return 1;
		if (this.seqNo < that.seqNo)
			return -1;
		if (this.seqNo > that.seqNo)
			return 1;
		return 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (time ^ (time >>> 32));
		result = prime * result + (int) (seqNo ^ (seqNo >>> 32));
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
		SimulatorEvent other = (SimulatorEvent) obj;
		if (time != other.time)
			return false;
		if (seqNo != other.seqNo)
			return false;
		return true;
	}
}
