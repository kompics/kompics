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

import java.util.LinkedList;

/**
 * The <code>LookupInfo</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class LookupInfo {

	private final ChordLookupRequest request;

	private long initiated, completed;

	private int hopCount;

	private final LinkedList<ChordAddress> hops;

	public LookupInfo(ChordLookupRequest request) {
		this.request = request;
		this.hopCount = 0;
		this.initiated = 0;
		this.completed = 0;
		this.hops = new LinkedList<ChordAddress>();
	}

	public int getHopCount() {
		return hopCount;
	}

	public void appendHop(ChordAddress hop) {
		this.hopCount++;
		this.hops.add(hop);
	}

	public ChordLookupRequest getRequest() {
		return request;
	}

	public void initiatedNow() {
		initiated = System.currentTimeMillis();
	}

	public void completedNow() {
		completed = System.currentTimeMillis();
	}

	public long getDuration() {
		return completed - initiated;
	}

	public LinkedList<ChordAddress> getHops() {
		return hops;
	}

	@Override
	public String toString() {
		return "HOPS=" + hopCount + " DURATION=" + (completed - initiated) + " INITIATED=" + initiated;
	}
}
