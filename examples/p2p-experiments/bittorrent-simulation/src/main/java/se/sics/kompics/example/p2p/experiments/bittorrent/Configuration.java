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
package se.sics.kompics.example.p2p.experiments.bittorrent;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.cdn.bittorrent.BitTorrentConfiguration;
import se.sics.kompics.p2p.cdn.bittorrent.TorrentMetadata;
import se.sics.kompics.p2p.cdn.bittorrent.address.BitTorrentAddress;

/**
 * The <code>Configuration</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class Configuration implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2137884866865661909L;

	public InetAddress ip = null;
	{
		try {
			ip = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
		}
	}
	int networkPort = 8081;
	int webPort = 8080;
	int trackerId = Integer.MAX_VALUE;

	BitTorrentAddress trackerAddress = new BitTorrentAddress(new Address(ip,
			networkPort, trackerId), BigInteger.valueOf(trackerId));

	// TODO vary these parameters
	boolean selfishPeers = false; // peers leave swarm after compelting download
	int initialSeeds = 1; // number of initial seeds
	int leechers = 16; // number of initial leechers
	int pipelineLength = 5; // the number of outstanding block requests
	int activePeersCount = 4; // number of peers a peer uploads to at one time

	// torrent
	int pieceSize = 256 * 1024;
	int pieceCount = 1024;

	BitTorrentConfiguration btConfiguration = new BitTorrentConfiguration(
			activePeersCount, 20, 40, 80, 16 * 1024, 4, pipelineLength, 10000,
			60000, 20000, 30000, 20000, 2 * 60 * 1000 /* 2 mins */, selfishPeers);

	TorrentMetadata torrent = new TorrentMetadata(BigInteger.TEN,
			trackerAddress, pieceCount, pieceSize);

	public void set() throws IOException {
		String c = File.createTempFile("bittorrent.", ".conf")
				.getAbsolutePath();
		btConfiguration.store(c);
		System.setProperty("bittorrent.configuration", c);

		c = File.createTempFile("torrent.", ".meta").getAbsolutePath();
		torrent.store(c);
		System.setProperty("torrent.metadata", c);
	}
}
