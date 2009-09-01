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

import java.math.BigInteger;

/**
 * The <code>CyclonConfiguration</code> class contains configuration parameters
 * for Cyclon.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class CyclonConfiguration {

	/**
	 * the number of descriptors exchanged during a shuffle.
	 */
	private final int shuffleLength;

	/**
	 * the size of the cache of each Cyclon node.
	 */
	private final int cacheSize;

	/**
	 * the number of milliseconds between two consecutive shuffles initiated by
	 * a node.
	 */
	private final long shufflePeriod;

	/**
	 * the number of milliseconds after which a node that does not respond to a
	 * shuffle request is considered dead.
	 */
	private final long shuffleTimeout;

	/**
	 * used by the simulator as the range of identifiers assigned to Cyclon
	 * nodes.
	 */
	private final BigInteger identifierSpaceSize;

	/**
	 * the number of peers that should be requested from the bootstrap service
	 * by a new Cyclon node.
	 */
	private final int bootstrapRequestPeerCount;

	public CyclonConfiguration(int shuffleLength, int cacheSize,
			long shufflePeriod, long shuffleTimeout,
			BigInteger identifierSpaceSize, int bootstrapRequestPeerCount) {
		super();
		this.shuffleLength = shuffleLength;
		this.cacheSize = cacheSize;
		this.shufflePeriod = shufflePeriod;
		this.shuffleTimeout = shuffleTimeout;
		this.identifierSpaceSize = identifierSpaceSize;
		this.bootstrapRequestPeerCount = bootstrapRequestPeerCount;
	}

	public int getShuffleLength() {
		return shuffleLength;
	}

	public int getCacheSize() {
		return cacheSize;
	}

	public long getShufflePeriod() {
		return shufflePeriod;
	}

	public long getShuffleTimeout() {
		return shuffleTimeout;
	}

	public BigInteger getIdentifierSpaceSize() {
		return identifierSpaceSize;
	}

	public int getBootstrapRequestPeerCount() {
		return bootstrapRequestPeerCount;
	}
}
