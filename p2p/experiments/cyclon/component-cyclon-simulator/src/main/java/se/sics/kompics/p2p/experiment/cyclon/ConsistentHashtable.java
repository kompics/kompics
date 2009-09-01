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
package se.sics.kompics.p2p.experiment.cyclon;

import java.util.TreeMap;

/**
 * The <code>ConsistentHashtable</code> class is used by the
 * <code>CyclonSimulator</code> to keep track of the existing peers, and
 * generate alternative (successor) identifiers for new peers if the randomly
 * generated identifier is already used. Also used to determine the peer to fail
 * based on a randomly generated identifier.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class ConsistentHashtable<K> {

	private final TreeMap<K, K> buckets = new TreeMap<K, K>();

	public ConsistentHashtable() {
		super();
	}

	/**
	 * Add a node to the ring.
	 * 
	 * @param key
	 * @throws
	 * @return the previous value associated with key, or null if there was no
	 *         mapping for key.
	 */
	public K addNode(K node) {
		return buckets.put(node, node);
	}

	/**
	 * Remove a node from the ring.
	 * 
	 * @param key
	 * @return null if there was mapping for the Node.
	 * @throws
	 */
	public K removeNode(K node) {
		return buckets.remove(node);
	}

	/**
	 * @param key
	 * @return the Node as a BigInteger, or null if there was mapping for the
	 *         Node.
	 */
	public K getNode(K key) {

		if (buckets.isEmpty()) {
			return null;
		}
		if (!buckets.containsKey(key)) {
			// returns the first key greater than or equal to supplied key,
			// or null if no such key.
			key = buckets.ceilingKey(key);
			// if no key found, go to start and return first key in the ring.
			if (key == null) {
				key = buckets.firstKey();
			}
		}
		return buckets.get(key);
	}

	public int size() {
		return buckets.size();
	}
}
