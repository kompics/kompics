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
package se.sics.kompics.p2p.overlay.cyclon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * The <code>Cache</code> class is implementing the local cache of each Cyclon
 * node.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class Cache {

	private Comparator<CacheEntry> comparatorByAge = new Comparator<CacheEntry>() {
		public int compare(CacheEntry o1, CacheEntry o2) {
			if (o1.getDescriptor().getAge() > o2.getDescriptor().getAge()) {
				return 1;
			} else if (o1.getDescriptor().getAge() < o2.getDescriptor()
					.getAge()) {
				return -1;
			} else {
				return 0;
			}
		}
	};

	/**
	 * the cache size
	 */
	private final int size;

	private final CyclonAddress self;

	private ArrayList<CacheEntry> entries;

	/**
	 * indexing cache entries by Cyclon addresses
	 */
	private HashMap<CyclonAddress, CacheEntry> d2e;

	private Random random = new Random();

	public Cache(int size, CyclonAddress self) {
		super();
		this.self = self;
		this.size = size;
		this.entries = new ArrayList<CacheEntry>();
		this.d2e = new HashMap<CyclonAddress, CacheEntry>();
	}

	public void incrementDescriptorAges() {
		for (CacheEntry entry : entries) {
			entry.getDescriptor().incrementAndGetAge();
		}
	}

	/**
	 * called at shuffle initiator upon initiating shuffle request
	 */
	public CyclonAddress selectPeerToShuffleWith() {
		if (entries.isEmpty()) {
			return null;
		}
		CacheEntry oldestEntry = Collections.max(entries, comparatorByAge);
		removeEntry(oldestEntry);
		return oldestEntry.getDescriptor().getCyclonAddress();
	}

	/**
	 * called at shuffle initiator upon initiating shuffle request
	 */
	public ArrayList<CyclonNodeDescriptor> selectToSendAtActive(int count,
			CyclonAddress destinationPeer) {
		ArrayList<CacheEntry> randomEntries = generateRandomSample(count);

		ArrayList<CyclonNodeDescriptor> descriptors = new ArrayList<CyclonNodeDescriptor>();
		for (CacheEntry cacheEntry : randomEntries) {
			cacheEntry.sentTo(destinationPeer);
			descriptors.add(cacheEntry.getDescriptor());
		}
		return descriptors;
	}

	/**
	 * called at shuffle receiver upon receiving shuffle request
	 */
	public ArrayList<CyclonNodeDescriptor> selectToSendAtPassive(int count,
			CyclonAddress destinationPeer) {
		ArrayList<CacheEntry> randomEntries = generateRandomSample(count);
		ArrayList<CyclonNodeDescriptor> descriptors = new ArrayList<CyclonNodeDescriptor>();
		for (CacheEntry cacheEntry : randomEntries) {
			cacheEntry.sentTo(destinationPeer);
			descriptors.add(cacheEntry.getDescriptor());
		}
		return descriptors;
	}

	/**
	 * called at shuffle receiver upon receiving shuffle request called at
	 * shuffle initiator upon receiving shuffle response
	 */
	public void selectToKeep(CyclonAddress from,
			ArrayList<CyclonNodeDescriptor> descriptors) {

		LinkedList<CacheEntry> entriesSentToThisPeer = new LinkedList<CacheEntry>();
		for (CacheEntry cacheEntry : entries) {
			if (cacheEntry.wasSentTo(from)) {
				entriesSentToThisPeer.add(cacheEntry);
			}
		}

		for (CyclonNodeDescriptor descriptor : descriptors) {
			if (self.equals(descriptor.getCyclonAddress())) {
				// do not keep descriptor of self
				continue;
			}
			if (d2e.containsKey(descriptor.getCyclonAddress())) {
				// we already have an entry for this peer. keep the youngest one
				CacheEntry entry = d2e.get(descriptor.getCyclonAddress());
				if (entry.getDescriptor().getAge() > descriptor.getAge()) {
					// we keep the lowest age descriptor
					removeEntry(entry);
					addEntry(new CacheEntry(descriptor));
					continue;
				} else {
					continue;
				}
			}
			if (entries.size() < size) {
				// fill an empty slot
				addEntry(new CacheEntry(descriptor));
				continue;
			}
			// replace one slot out of those sent to this peer
			CacheEntry sentEntry = entriesSentToThisPeer.poll();
			if (sentEntry != null) {
				removeEntry(sentEntry);
				addEntry(new CacheEntry(descriptor));
			}
		}
	}

	/**
	 * @return all peers from the cache.
	 */
	public final ArrayList<CyclonNodeDescriptor> getAll() {
		ArrayList<CyclonNodeDescriptor> descriptors = new ArrayList<CyclonNodeDescriptor>();
		for (CacheEntry cacheEntry : entries) {
			descriptors.add(cacheEntry.getDescriptor());
		}
		return descriptors;
	}

	/**
	 * Generates a list of random peers from the cache.
	 * 
	 * @param count
	 *            how many peers to generate.
	 * @return the list of random peers.
	 */
	public final List<CyclonAddress> getRandomPeers(int count) {
		ArrayList<CacheEntry> randomEntries = generateRandomSample(count);
		LinkedList<CyclonAddress> randomPeers = new LinkedList<CyclonAddress>();

		for (CacheEntry cacheEntry : randomEntries) {
			randomPeers.add(cacheEntry.getDescriptor().getCyclonAddress());
		}

		return randomPeers;
	}

	private final ArrayList<CacheEntry> generateRandomSample(int n) {
		ArrayList<CacheEntry> randomEntries;
		if (n >= entries.size()) {
			// return all entries
			randomEntries = new ArrayList<CacheEntry>(entries);
		} else {
			// return count random entries
			randomEntries = new ArrayList<CacheEntry>();
			// Don Knuth, The Art of Computer Programming, Algorithm S(3.4.2)
			int t = 0, m = 0, N = entries.size();
			while (m < n) {
				int x = random.nextInt(N - t);
				if (x < n - m) {
					randomEntries.add(entries.get(t));
					m += 1;
					t += 1;
				} else {
					t += 1;
				}
			}
		}
		return randomEntries;
	}

	private void addEntry(CacheEntry entry) {
		entries.add(entry);
		d2e.put(entry.getDescriptor().getCyclonAddress(), entry);
		checkSize();
	}

	private void removeEntry(CacheEntry entry) {
		entries.remove(entry);
		d2e.remove(entry.getDescriptor().getCyclonAddress());
		checkSize();
	}

	private void checkSize() {
		if (entries.size() != d2e.size())
			throw new RuntimeException("WHD " + entries.size() + " <> "
					+ d2e.size());
	}
}
