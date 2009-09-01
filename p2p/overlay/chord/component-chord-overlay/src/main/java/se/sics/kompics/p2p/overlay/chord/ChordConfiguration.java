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

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

/**
 * The <code>ChordConfiguration</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class ChordConfiguration {

	private final int log2RingSize;
	private final int successorListLength;
	private final long successorStabilizationPeriod;
	private final long fingerStabilizationPeriod;
	private final long rpcTimeout;
	private final int bootstrapRequestPeerCount;

	public ChordConfiguration(int log2RingSize, int successorListLength,
			long successorStabilizationPeriod, long fingerStabilizationPeriod,
			long rpcTimeout, int bootstrapRequestPeerCount) {
		super();
		this.log2RingSize = log2RingSize;
		this.successorListLength = successorListLength;
		this.successorStabilizationPeriod = successorStabilizationPeriod;
		this.fingerStabilizationPeriod = fingerStabilizationPeriod;
		this.rpcTimeout = rpcTimeout;
		this.bootstrapRequestPeerCount = bootstrapRequestPeerCount;
	}

	public int getLog2RingSize() {
		return log2RingSize;
	}

	public int getSuccessorListLength() {
		return successorListLength;
	}

	public long getSuccessorStabilizationPeriod() {
		return successorStabilizationPeriod;
	}

	public long getFingerStabilizationPeriod() {
		return fingerStabilizationPeriod;
	}

	public long getRpcTimeout() {
		return rpcTimeout;
	}

	public int getBootstrapRequestPeerCount() {
		return bootstrapRequestPeerCount;
	}

	public void store(String file) throws IOException {
		Properties p = new Properties();
		p.setProperty("log2.ring.size", "" + log2RingSize);
		p.setProperty("successor.list.length", "" + successorListLength);
		p.setProperty("successor.stabilization.period", ""
				+ successorStabilizationPeriod);
		p.setProperty("finger.stabilization.period", ""
				+ fingerStabilizationPeriod);
		p.setProperty("rpc.timeout", "" + rpcTimeout);
		p.setProperty("bootstrap.request.peer.count", ""
				+ bootstrapRequestPeerCount);

		Writer writer = new FileWriter(file);
		p.store(writer, "se.sics.kompics.p2p.overlay.chord");
	}

	public static ChordConfiguration load(String file) throws IOException {
		Properties p = new Properties();
		Reader reader = new FileReader(file);
		p.load(reader);

		int log2RingSize = Integer.parseInt("log2.ring.size");
		int successorListLength = Integer.parseInt("successor.list.length");
		long successorStabilizationPeriod = Long
				.parseLong("successor.stabilization.period");
		long fingerStabilizationPeriod = Long
				.parseLong("finger.stabilization.period");
		long rpcTimeout = Long.parseLong("rpc.timeout");
		int bootstrapRequestPeerCount = Integer
				.parseInt("bootstrap.request.peer.count");

		return new ChordConfiguration(log2RingSize, successorListLength,
				successorStabilizationPeriod, fingerStabilizationPeriod,
				rpcTimeout, bootstrapRequestPeerCount);
	}
}
