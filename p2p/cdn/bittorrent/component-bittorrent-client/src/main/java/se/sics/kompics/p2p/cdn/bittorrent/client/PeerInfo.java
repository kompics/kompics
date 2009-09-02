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

import java.util.Comparator;
import java.util.LinkedList;

import se.sics.kompics.p2p.cdn.bittorrent.BitTorrentConfiguration;
import se.sics.kompics.p2p.cdn.bittorrent.TorrentMetadata;
import se.sics.kompics.p2p.cdn.bittorrent.address.BitTorrentAddress;

/**
 * The <code>PeerInfo</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class PeerInfo {

	BitTorrentAddress peer;
	boolean amInterested;
	boolean peerInterested;
	boolean amChoking;

	/** The peer choking. */
	boolean peerChoking;
	boolean initiatedByMe;
	Bitfield pieces;
	TransferHistory downloadHistory; // download from peer to us
	TransferHistory uploadHistory; // upload from us to peer

	LinkedList<Block> requestPipeline;

	long lastUnchoked;
	int currentUploadRate; // from peer to us
	int currentDownloadRate; // from us to peer

	long lastKeepAliveReceivedAt;

	public PeerInfo(BitTorrentAddress peer, boolean initiatedByMe,
			BitTorrentConfiguration configuration, TorrentMetadata torrent) {
		super();
		this.peer = peer;
		this.initiatedByMe = initiatedByMe;
		amInterested = false;
		peerInterested = false;
		amChoking = true;
		peerChoking = true;
		pieces = new Bitfield(torrent.getPieceCount());
		downloadHistory = new TransferHistory(configuration
				.getTransferHistoryLength());
		uploadHistory = new TransferHistory(configuration
				.getTransferHistoryLength());
		requestPipeline = new LinkedList<Block>();
		lastKeepAliveReceivedAt = System.currentTimeMillis();
	}

	public void discardPiece(int piece) {
		LinkedList<Block> toRemove = new LinkedList<Block>();
		for (Block block : requestPipeline) {
			if (block.getPieceIndex() == piece)
				toRemove.add(block);
		}
		for (Block block : toRemove) {
			requestPipeline.remove(block);
		}
	}

	/**
	 * @return <code>true</code> if we are interested in this peer and we were
	 *         previously not interested and <code>false</code> otherwise.
	 */
	public boolean becameInteresting(Bitfield myPieces) {
		boolean before = amInterested;

		Bitfield peerHas = pieces.copy();
		peerHas.andNot(myPieces);
		amInterested = !peerHas.isEmpty();

		return !before && amInterested;
	}

	/**
	 * @return <code>true</code> if we are not interested in this peer and we
	 *         were previously interested and <code>false</code> otherwise.
	 */
	public boolean becameNotInteresting(Bitfield myPieces) {
		boolean before = amInterested;

		Bitfield peerHas = pieces.copy();
		peerHas.andNot(myPieces);
		amInterested = !peerHas.isEmpty();

		return before && !amInterested;
	}

	/**
	 * compute the rate of upload from this peer to us
	 * 
	 * @param window
	 */
	public void computeCurrentUploadRate(long window) {
		currentUploadRate = downloadHistory
				.getBlocksTranferredInTheLast(window);
	}

	/**
	 * compute the rate of download from us to this peer
	 * 
	 * @param window
	 */
	public void computeCurrentDownloadRate(long window) {
		currentDownloadRate = uploadHistory
				.getBlocksTranferredInTheLast(window);
	}

	public boolean isLastUnchokeInTheLast(long window) {
		long now = System.currentTimeMillis();
		return now - lastUnchoked < window;
	}

	/**
	 * sorts peers by their last unchoke time, most recent first. On a tie first
	 * comes the peer with higher download rate from us
	 */
	public static Comparator<PeerInfo> sortByLastUnchoke = new Comparator<PeerInfo>() {
		@Override
		public int compare(PeerInfo p1, PeerInfo p2) {
			if (p1.lastUnchoked < p2.lastUnchoked)
				return 1;
			if (p1.lastUnchoked > p2.lastUnchoked)
				return -1;
			if (p1.currentDownloadRate < p2.currentDownloadRate)
				return 1;
			if (p1.currentDownloadRate > p2.currentDownloadRate)
				return -1;
			return 0;
		};
	};

	/**
	 * sorts peers by their rate of upload to us. Highest upload rate first.
	 */
	public static Comparator<PeerInfo> sortByUploadRate = new Comparator<PeerInfo>() {
		@Override
		public int compare(PeerInfo p1, PeerInfo p2) {
			if (p1.currentUploadRate < p2.currentUploadRate)
				return 1;
			if (p1.currentUploadRate > p2.currentUploadRate)
				return -1;
			return 0;
		};
	};

	/**
	 * sorts peers by their rate of download from us. Highest download rate
	 * first.
	 */
	public static Comparator<PeerInfo> sortByDownloadRate = new Comparator<PeerInfo>() {
		@Override
		public int compare(PeerInfo p1, PeerInfo p2) {
			if (p1.currentDownloadRate < p2.currentDownloadRate)
				return 1;
			if (p1.currentDownloadRate > p2.currentDownloadRate)
				return -1;
			return 0;
		};
	};
}
