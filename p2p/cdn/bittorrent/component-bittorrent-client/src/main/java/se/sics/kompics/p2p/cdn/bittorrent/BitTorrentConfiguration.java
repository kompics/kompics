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
package se.sics.kompics.p2p.cdn.bittorrent;

import java.io.Serializable;

/**
 * The <code>BitTorrentConfiguration</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class BitTorrentConfiguration implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4497570674238884004L;

	private final int activePeersCount;

	private final int minPeersThreshold;

	private final int maxInitiatedConnections;

	private final int maxPeers;

	private final int blockSize;

	private final int randomPieceCount;

	private final int requestPipelineLength;

	/**
	 * the time period between two choking rounds
	 */
	private final int chokingPeriod;

	/**
	 * after this peer, entries are discarded from the download history
	 */
	private final int transferHistoryLength;

	/**
	 * the width of the time window used to compute the current download rate
	 * from a peer and the current upload rate to a peer
	 */
	private final int transferRateWindow;

	/**
	 * peer is snubbed if it didn't upload any block to us in this time window
	 */
	private final int snubbedWindow;

	/**
	 * in the seed choke algorithm, priority is given to the peers that were
	 * unchoked in this time window
	 */
	private final int unchokeWindow;

	private final long keepAlivePeriod;

	public BitTorrentConfiguration(int activePeersCount, int minPeersThreshold,
			int maxInitiatedConnections, int maxPeers, int blockSize,
			int randomPieceCount, int requestPipelineLength, int chokingPeriod,
			int transferHistoryLength, int transferRateWindow,
			int snubbedWindow, int unchokeWindow, long keepAlivePeriod) {
		super();
		this.activePeersCount = activePeersCount;
		this.minPeersThreshold = minPeersThreshold;
		this.maxInitiatedConnections = maxInitiatedConnections;
		this.maxPeers = maxPeers;
		this.blockSize = blockSize;
		this.randomPieceCount = randomPieceCount;
		this.requestPipelineLength = requestPipelineLength;
		this.chokingPeriod = chokingPeriod;
		this.transferHistoryLength = transferHistoryLength;
		this.transferRateWindow = transferRateWindow;
		this.snubbedWindow = snubbedWindow;
		this.unchokeWindow = unchokeWindow;
		this.keepAlivePeriod = keepAlivePeriod;
	}

	public int getActivePeersCount() {
		return activePeersCount;
	}

	public int getMinPeersThreshold() {
		return minPeersThreshold;
	}

	public int getMaxInitiatedConnections() {
		return maxInitiatedConnections;
	}

	public int getMaxPeers() {
		return maxPeers;
	}

	public int getBlockSize() {
		return blockSize;
	}

	public int getRandomPieceCount() {
		return randomPieceCount;
	}

	public int getRequestPipelineLength() {
		return requestPipelineLength;
	}

	public int getChokingPeriod() {
		return chokingPeriod;
	}

	public int getTransferHistoryLength() {
		return transferHistoryLength;
	}

	public int getTransferRateWindow() {
		return transferRateWindow;
	}

	public int getSnubbedWindow() {
		return snubbedWindow;
	}

	public int getUnchokeWindow() {
		return unchokeWindow;
	}

	public long getKeepAlivePeriod() {
		return keepAlivePeriod;
	}
}
