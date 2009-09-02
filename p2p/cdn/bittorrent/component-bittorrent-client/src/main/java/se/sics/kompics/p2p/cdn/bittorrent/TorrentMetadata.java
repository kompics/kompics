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

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.Properties;

import se.sics.kompics.address.Address;
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

	public void store(String file) throws IOException {
		Properties p = new Properties();
		p.setProperty("tracker.ip", ""
				+ tracker.getPeerAddress().getIp().getHostAddress());
		p.setProperty("tracker.port", "" + tracker.getPeerAddress().getPort());
		p.setProperty("tracker.id", "" + tracker.getPeerAddress().getId());
		p.setProperty("tracker.peer.id", "" + tracker.getPeerId());
		p.setProperty("torrent.id", "" + torrentId);
		p.setProperty("piece.count", "" + pieceCount);
		p.setProperty("piece.size", "" + pieceSize);

		Writer writer = new FileWriter(file);
		p.store(writer, "se.sics.kompics.p2p.bittorrent.torrent.metadata");
	}

	public static TorrentMetadata load(String file) throws IOException {
		Properties p = new Properties();
		Reader reader = new FileReader(file);
		p.load(reader);

		InetAddress ip = InetAddress.getByName(p.getProperty("tracker.ip"));
		int port = Integer.parseInt(p.getProperty("tracker.port"));
		int id = Integer.parseInt(p.getProperty("tracker.id"));
		BigInteger trackerBigId = new BigInteger(p
				.getProperty("tracker.peer.id"));

		BitTorrentAddress tracker = new BitTorrentAddress(new Address(ip, port,
				id), trackerBigId);
		BigInteger torrentId = new BigInteger(p.getProperty("torrent.id"));
		int pieceCount = Integer.parseInt(p.getProperty("piece.count"));
		int pieceSize = Integer.parseInt(p.getProperty("piece.size"));

		return new TorrentMetadata(torrentId, tracker, pieceCount, pieceSize);
	}
}
