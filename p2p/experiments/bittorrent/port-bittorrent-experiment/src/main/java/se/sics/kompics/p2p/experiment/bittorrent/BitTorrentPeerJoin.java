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
package se.sics.kompics.p2p.experiment.bittorrent;

import java.math.BigInteger;

import se.sics.kompics.Event;
import se.sics.kompics.p2p.cdn.bittorrent.client.Bitfield;

/**
 * The <code>BitTorrentPeerJoin</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 */
public final class BitTorrentPeerJoin extends Event {

	private final BigInteger peerId;

	private final long downloadBw; // in bytes per second

	private final long uploadBw; // in bytes per second

	private final Bitfield downloaded;

	/**
	 * Instantiates a new bit torrent peer join.
	 * 
	 * @param peerId the peer id
	 * @param downloadBw the download bw
	 * @param uploadBw the upload bw
	 * @param downloaded the downloaded
	 */
	public BitTorrentPeerJoin(BigInteger peerId, long downloadBw,
			long uploadBw, Bitfield downloaded) {
		super();
		this.peerId = peerId;
		this.downloadBw = downloadBw;
		this.uploadBw = uploadBw;
		this.downloaded = downloaded;
	}

	@Override
	public String toString() {
		return "Join@" + peerId;
	}

	public BigInteger getPeerId() {
		return peerId;
	}

	public long getDownloadBw() {
		return downloadBw;
	}

	public long getUploadBw() {
		return uploadBw;
	}

	public Bitfield getDownloaded() {
		return downloaded;
	}
}
