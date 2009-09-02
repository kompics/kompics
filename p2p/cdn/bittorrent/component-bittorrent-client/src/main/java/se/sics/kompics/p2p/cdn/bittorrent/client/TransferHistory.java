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

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The <code>TransferHistory</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class TransferHistory {

	private final long length;

	private final TreeMap<Long, Integer> history;

	public TransferHistory(long length) {
		super();
		this.length = length;
		history = new TreeMap<Long, Integer>();
	}

	public void transferredBlock() {
		long now = System.currentTimeMillis();
		Integer count = history.get(now);
		if (count == null) {
			history.put(now, 1);
		} else {
			history.put(now, 1 + count);
		}
		cleanOldHistory(now);
	}

	public int getBlocksTranferredInTheLast(long millis) {
		long now = System.currentTimeMillis();
		long minTs = now - millis;

		int count = 0;
		SortedMap<Long, Integer> recent = history.tailMap(minTs);
		for (Integer blocks : recent.values()) {
			count += blocks;
		}

		cleanOldHistory(now);
		return count;
	}

	private void cleanOldHistory(long now) {
		if (history.isEmpty()) {
			return;
		}
		long minTs = now - length;
		Long oldest = history.firstKey();
		while (oldest != null && oldest < minTs) {
			history.remove(oldest);
			if (history.isEmpty()) {
				return;
			}
			oldest = history.firstKey();
		}
	}
}
