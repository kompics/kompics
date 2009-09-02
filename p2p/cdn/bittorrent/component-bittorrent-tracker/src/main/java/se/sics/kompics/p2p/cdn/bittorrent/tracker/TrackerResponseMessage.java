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
package se.sics.kompics.p2p.cdn.bittorrent.tracker;

import java.util.LinkedList;

import se.sics.kompics.p2p.cdn.bittorrent.address.BitTorrentAddress;
import se.sics.kompics.p2p.cdn.bittorrent.message.BitTorrentMessage;

/**
 * The <code>TrackerResponseMessage</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class TrackerResponseMessage extends BitTorrentMessage {

	private static final long serialVersionUID = -2972648488355858152L;

	private final String failureReason, warningMessage;

	private final int interval, complete, incomplete;

	private final LinkedList<BitTorrentAddress> peers;

	public TrackerResponseMessage(BitTorrentAddress source,
			BitTorrentAddress destination, String failureReason,
			String warningMessage, int interval, int complete, int incomplete,
			LinkedList<BitTorrentAddress> peers) {
		super(source, destination);
		this.failureReason = failureReason;
		this.warningMessage = warningMessage;
		this.interval = interval;
		this.complete = complete;
		this.incomplete = incomplete;
		this.peers = peers;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public String getWarningMessage() {
		return warningMessage;
	}

	public int getInterval() {
		return interval;
	}

	public int getComplete() {
		return complete;
	}

	public int getIncomplete() {
		return incomplete;
	}

	public LinkedList<BitTorrentAddress> getPeers() {
		return peers;
	}

	@Override
	public int getSize() {
		return 0;
	}
}
