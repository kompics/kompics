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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.cdn.bittorrent.address.BitTorrentAddress;
import se.sics.kompics.timer.Timer;

/**
 * The <code>BitTorrentTracker</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class BitTorrentTracker extends ComponentDefinition {

	Positive<Network> network = positive(Network.class);
	Positive<Timer> timer = positive(Timer.class);

	private static final Logger logger = LoggerFactory
			.getLogger(BitTorrentTracker.class);

	private HashSet<BitTorrentAddress> cache;

	private BitTorrentAddress self;

	public BitTorrentTracker() {
		cache = new HashSet<BitTorrentAddress>();

		subscribe(handleInit, control);
		subscribe(handleRequest, network);
	}

	Handler<BitTorrentTrackerInit> handleInit = new Handler<BitTorrentTrackerInit>() {
		public void handle(BitTorrentTrackerInit init) {
			logger.info("INIT");

			self = init.getTrackerAddress();
		}
	};

	Handler<TrackerRequestMessage> handleRequest = new Handler<TrackerRequestMessage>() {
		public void handle(TrackerRequestMessage event) {
			BitTorrentAddress peer = event.getBitTorrentSource();
			int n = event.getNumWant();
			if (n > 0) {
				LinkedList<BitTorrentAddress> peers = new LinkedList<BitTorrentAddress>(
						cache);
				Collections.shuffle(peers);
				while (peers.size() > n) {
					peers.removeLast();
				}

				TrackerResponseMessage responseMessage = new TrackerResponseMessage(
						self, peer, "", "", 0, 0, 0, peers);
				trigger(responseMessage, network);
				logger.debug("Sent {} peers", peers.size());
			}
			cache.add(peer);
		}
	};
}
