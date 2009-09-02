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
package se.sics.kompics.p2p.cdn.bittorrent.message;

import se.sics.kompics.network.Message;
import se.sics.kompics.p2p.cdn.bittorrent.address.BitTorrentAddress;

/**
 * The <code>BitTorrentMessage</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: BitTorrentMessage.java 1072 2009-08-28 09:03:02Z Cosmin $
 */
public abstract class BitTorrentMessage extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6815596147580962155L;

	private final BitTorrentAddress source;

	private final BitTorrentAddress destination;

	public BitTorrentMessage(BitTorrentAddress source,
			BitTorrentAddress destination) {
		super(source.getPeerAddress(), destination.getPeerAddress());
		this.source = source;
		this.destination = destination;
	}

	/**
	 * Gets the bit torrent destination.
	 * 
	 * @return the bit torrent destination
	 */
	public BitTorrentAddress getBitTorrentDestination() {
		return destination;
	}

	public BitTorrentAddress getBitTorrentSource() {
		return source;
	}
	
	public abstract int getSize();
}
