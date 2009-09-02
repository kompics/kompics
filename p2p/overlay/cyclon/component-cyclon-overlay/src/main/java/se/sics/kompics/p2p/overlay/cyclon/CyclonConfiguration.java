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

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigInteger;
import java.util.Properties;

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

	public void store(String file) throws IOException {
		Properties p = new Properties();
		p.setProperty("shuffle.length", "" + shuffleLength);
		p.setProperty("cache.size", "" + cacheSize);
		p.setProperty("shuffle.period", "" + shufflePeriod);
		p.setProperty("shuffle.timeout", "" + shuffleTimeout);
		p.setProperty("id.space.size", "" + identifierSpaceSize);
		p.setProperty("bootstrap.request.peer.count", ""
				+ bootstrapRequestPeerCount);

		Writer writer = new FileWriter(file);
		p.store(writer, "se.sics.kompics.p2p.overlay.cyclon");
	}

	public static CyclonConfiguration load(String file) throws IOException {
		Properties p = new Properties();
		Reader reader = new FileReader(file);
		p.load(reader);

		int shuffleLength = Integer.parseInt(p.getProperty("shuffle.length"));
		int cacheSize = Integer.parseInt(p.getProperty("cache.size"));
		long shufflePeriod = Long.parseLong(p.getProperty("shuffle.period"));
		long shuffleTimeout = Long.parseLong(p.getProperty("shuffle.timeout"));
		BigInteger identifierSpaceSize = new BigInteger(p
				.getProperty("id.space.size"));
		int bootstrapRequestPeerCount = Integer.parseInt(p
				.getProperty("bootstrap.request.peer.count"));

		return new CyclonConfiguration(shuffleLength, cacheSize, shufflePeriod,
				shuffleTimeout, identifierSpaceSize, bootstrapRequestPeerCount);
	}
}
