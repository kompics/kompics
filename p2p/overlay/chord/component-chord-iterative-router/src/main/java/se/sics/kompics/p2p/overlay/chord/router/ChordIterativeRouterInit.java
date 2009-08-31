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
package se.sics.kompics.p2p.overlay.chord.router;

import se.sics.kompics.Init;
import se.sics.kompics.p2p.overlay.chord.ChordAddress;

/**
 * The <code>ChordIterativeRouterInit</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class ChordIterativeRouterInit extends Init {

	private final int log2RingSize;
	private final long fingerStabilizationPeriod;
	private final long rpcTimeout;
	private final ChordAddress self;

	public ChordIterativeRouterInit(int log2RingSize, long stabilizationPeriod,
			long rpcTimeout, ChordAddress self) {
		super();
		this.log2RingSize = log2RingSize;
		this.rpcTimeout = rpcTimeout;
		this.fingerStabilizationPeriod = stabilizationPeriod;
		this.self = self;
	}

	public final int getLog2RingSize() {
		return log2RingSize;
	}

	public final long getRpcTimeout() {
		return rpcTimeout;
	}

	public final long getFingerStabilizationPeriod() {
		return fingerStabilizationPeriod;
	}

	public final ChordAddress getSelf() {
		return self;
	}
}
