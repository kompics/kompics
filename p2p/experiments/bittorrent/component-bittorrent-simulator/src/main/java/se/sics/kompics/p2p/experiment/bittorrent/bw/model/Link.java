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
package se.sics.kompics.p2p.experiment.bittorrent.bw.model;

import se.sics.kompics.p2p.cdn.bittorrent.message.BitTorrentMessage;

/**
 * The <code>Link</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class Link {

	// link capacities are given in bytes per second
	private final long capacity;

	private long lastExitTime;

	public Link(long capacity) {
		this.capacity = capacity;
		this.lastExitTime = System.currentTimeMillis();
	}

	/**
	 * @param message
	 * @return the delay in ms for this message
	 */
	public long addMessage(BitTorrentMessage message) {
		double size = message.getSize();
		double capacityPerMs = ((double) capacity) / 1000;
		long bwDelayMs = (long) (size / capacityPerMs);
		long now = System.currentTimeMillis();

		if (now >= lastExitTime) {
			// the pipe is empty
			lastExitTime = now + bwDelayMs;
		} else {
			// the pipe has some messages and the last message's exit time is
			// stored in lastExitTime
			lastExitTime = lastExitTime + bwDelayMs;
			//
			bwDelayMs = lastExitTime - now;
		}
		return bwDelayMs;
	}
}
