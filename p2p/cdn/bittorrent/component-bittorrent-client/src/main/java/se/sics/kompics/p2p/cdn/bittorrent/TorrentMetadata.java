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
import java.math.BigInteger;

import se.sics.kompics.p2p.cdn.bittorrent.address.BitTorrentAddress;

/**
 * The <code>TorrentMetadata</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class TorrentMetadata implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5233986276890423567L;

	private final BigInteger torrentId;

	private final int pieceCount;

	private final int pieceSize;

	private final BitTorrentAddress tracker;

	public TorrentMetadata(BigInteger torrentId, BitTorrentAddress tracker,
			int pieceCount, int pieceSize) {
		super();
		this.torrentId = torrentId;
		this.tracker = tracker;
		this.pieceCount = pieceCount;
		this.pieceSize = pieceSize;
	}

	public BigInteger getTorrentId() {
		return torrentId;
	}

	public BitTorrentAddress getTracker() {
		return tracker;
	}

	public int getPieceCount() {
		return pieceCount;
	}

	public int getPieceSize() {
		return pieceSize;
	}
}
