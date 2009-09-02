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

import java.util.BitSet;

import se.sics.kompics.p2p.cdn.bittorrent.address.BitTorrentAddress;

/**
 * The <code>PieceInTransit</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class PieceInTransit {

	int pieceIndex;

	BitSet receivedBlocks;

	BitSet requestedBlocks;

	int totalBlocks;

	long lastBlockReceivedAt, lastBlockRequestedAt;

	BitTorrentAddress fromPeer;

	public PieceInTransit(int piece, int totalBlocks, BitTorrentAddress peer) {
		this.pieceIndex = piece;
		this.totalBlocks = totalBlocks;
		this.requestedBlocks = new BitSet(totalBlocks);
		this.receivedBlocks = new BitSet(totalBlocks);
		this.fromPeer = peer;
		lastBlockReceivedAt = System.currentTimeMillis();
		lastBlockRequestedAt = lastBlockReceivedAt;
	}

	/**
	 * @param blockIndex
	 * @return <code>true</code> if all blocks have been received and
	 *         <code>false</code> otherwise.
	 */
	public boolean blockReceived(int blockIndex) {
		lastBlockReceivedAt = System.currentTimeMillis();
		receivedBlocks.set(blockIndex);
		return receivedBlocks.cardinality() == totalBlocks;
	}

	public int getNextBlockToRequest() {
		lastBlockRequestedAt = System.currentTimeMillis();
		int nextBlock = requestedBlocks.length();
		if (nextBlock < totalBlocks) {
			requestedBlocks.set(nextBlock);
		} else {
			nextBlock = -1;
		}
		return nextBlock;
	}

	public boolean isStalePiece(long threshold) {
		long now = System.currentTimeMillis();
		return now - lastBlockRequestedAt > threshold;
	}

	public BitTorrentAddress getFromPeer() {
		return fromPeer;
	}

	public void resetRequestedBlocks() {
		requestedBlocks = new BitSet(totalBlocks);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < totalBlocks; i++) {
			sb.append(requestedBlocks.get(i) ? 1 : 0);
		}
		System.err.println("RST " + sb.toString());
		// requestedBlocks = (BitSet) receivedBlocks.clone();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + pieceIndex;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PieceInTransit other = (PieceInTransit) obj;
		if (pieceIndex != other.pieceIndex)
			return false;
		return true;
	}
}
