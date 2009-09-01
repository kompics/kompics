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
package se.sics.kompics.p2p.overlay.cyclon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

/**
 * The <code>Cyclon</code> class is component implementing the Cyclon protocol.
 * 
 * TODO You have to extend this class to implement the Cyclon join protocol based on
 * random walks. You define your own messages and handlers for the random walk.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class Cyclon extends ComponentDefinition {

	Negative<CyclonPeerSampling> random = negative(CyclonPeerSampling.class);
	Negative<CyclonStatus> status = negative(CyclonStatus.class);
	Positive<Network> network = positive(Network.class);
	Positive<Timer> timer = positive(Timer.class);

	private Logger logger;
	private CyclonAddress self;

	private int shuffleLength;
	private int cacheSize;
	private long shufflePeriod;
	private long shuffleTimeout;

	private Cache cache;

	private boolean joining;

	private HashMap<UUID, CyclonAddress> outstandingShuffles;

	public Cyclon() {
		outstandingShuffles = new HashMap<UUID, CyclonAddress>();

		subscribe(handleInit, control);

		subscribe(handleJoin, random);
		subscribe(handlePeerRequest, random);
		subscribe(handleNeighborsRequest, status);

		subscribe(handleInitiateShuffle, timer);
		subscribe(handleShuffleRequest, network);
		subscribe(handleShuffleResponse, network);
		subscribe(handleShuffleTimeout, timer);
	}

	Handler<CyclonInit> handleInit = new Handler<CyclonInit>() {
		public void handle(CyclonInit init) {
			shuffleLength = init.getConfiguration().getShuffleLength();
			cacheSize = init.getConfiguration().getCacheSize();
			shufflePeriod = init.getConfiguration().getShufflePeriod();
			shuffleTimeout = init.getConfiguration().getShuffleTimeout();
		}
	};

	/**
	 * handles a request to join a Cyclon network using a set of introducer
	 * nodes provided in the Join event.
	 */
	Handler<Join> handleJoin = new Handler<Join>() {
		public void handle(Join event) {
			self = event.getSelf();
			cache = new Cache(cacheSize, self);

			logger = LoggerFactory.getLogger(getClass().getName() + "@"
					+ self.getCyclonId());

			logger.error("JOIN through {}", event.getCyclonInsiders());

			LinkedList<CyclonAddress> insiders = event.getCyclonInsiders();

			if (insiders.size() == 0) {
				// I am the first peer
				trigger(new JoinCompleted(self), random);

				// schedule shuffling
				SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(
						shufflePeriod, shufflePeriod);
				spt.setTimeoutEvent(new InitiateShuffle(spt));
				trigger(spt, timer);
				return;
			}

			initiateShuffle(1, insiders.poll());
			joining = true;
		}
	};

	/**
	 * initiates a shuffle of size <code>shuffleSize</code>. Called either
	 * during the join protocol with a <code>shuffleSize</code> of 1, or
	 * periodically to initiate regular shuffles.
	 * 
	 * @param shuffleSize
	 * @param peer
	 */
	private void initiateShuffle(int shuffleSize, CyclonAddress peer) {
		ArrayList<CyclonNodeDescriptor> descriptors = cache
				.selectToSendAtActive(shuffleSize - 1, peer);

		descriptors.add(new CyclonNodeDescriptor(self));
		DescriptorBuffer buffer = new DescriptorBuffer(self, descriptors);

		ScheduleTimeout st = new ScheduleTimeout(shuffleTimeout);
		st.setTimeoutEvent(new ShuffleTimeout(st, peer));
		UUID timeoutId = st.getTimeoutEvent().getTimeoutId();

		outstandingShuffles.put(timeoutId, peer);
		ShuffleRequest request = new ShuffleRequest(timeoutId, buffer, self,
				peer);

		trigger(st, timer);
		trigger(request, network);
	}

	/**
	 * Handles a request for a random sample of peers, implementing a Peer
	 * Sampling service.
	 */
	Handler<CyclonGetPeersRequest> handlePeerRequest = new Handler<CyclonGetPeersRequest>() {
		public void handle(CyclonGetPeersRequest event) {
			int count = event.getMaxPeers();
			List<CyclonAddress> peers = cache.getRandomPeers(count);

			CyclonGetPeersResponse response = new CyclonGetPeersResponse(event,
					peers);
			trigger(response, random);
		}
	};

	/**
	 * Handles a request for the current neighbors of this Cyclon node.
	 */
	Handler<CyclonNeighborsRequest> handleNeighborsRequest = new Handler<CyclonNeighborsRequest>() {
		public void handle(CyclonNeighborsRequest event) {
			CyclonNeighbors neighbors = new CyclonNeighbors(self,
					cache != null ? cache.getAll()
							: new ArrayList<CyclonNodeDescriptor>());
			trigger(new CyclonNeighborsResponse(event, neighbors), status);
		}
	};

	/**
	 * Periodically, will initiate regular shuffles. This is the first half of
	 * the "active thread" of the Cyclon specification.
	 */
	Handler<InitiateShuffle> handleInitiateShuffle = new Handler<InitiateShuffle>() {
		public void handle(InitiateShuffle event) {
			cache.incrementDescriptorAges();
			CyclonAddress peer = cache.selectPeerToShuffleWith();
			if (peer != null) {
				initiateShuffle(shuffleLength, peer);
			}
		}
	};

	/**
	 * Handles a shuffle request message. This is the "passive thread" of the
	 * Cyclon specification.
	 */
	Handler<ShuffleRequest> handleShuffleRequest = new Handler<ShuffleRequest>() {
		public void handle(ShuffleRequest event) {
			CyclonAddress peer = event.getCyclonSource();

			DescriptorBuffer receivedBuffer = event.getBuffer();
			DescriptorBuffer toSendBuffer = new DescriptorBuffer(self, cache
					.selectToSendAtPassive(receivedBuffer.getSize(), peer));

			cache.selectToKeep(peer, receivedBuffer.getDescriptors());

			logger.debug("SHUFFLE_REQ from {}. r={} s={}", new Object[] { peer,
					receivedBuffer.getSize(), toSendBuffer.getSize() });

			ShuffleResponse response = new ShuffleResponse(
					event.getRequestId(), toSendBuffer, self, peer);
			trigger(response, network);
		}
	};

	/**
	 * Handles a shuffle response message. This is the second half of the
	 * "active thread" of the Cyclon specification.
	 */
	Handler<ShuffleResponse> handleShuffleResponse = new Handler<ShuffleResponse>() {
		public void handle(ShuffleResponse event) {
			if (joining) {
				joining = false;
				trigger(new JoinCompleted(self), random);

				// schedule shuffling
				SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(
						shufflePeriod, shufflePeriod);
				spt.setTimeoutEvent(new InitiateShuffle(spt));
				trigger(spt, timer);
			}

			// cancel shuffle timeout
			UUID shuffleId = event.getRequestId();
			if (outstandingShuffles.containsKey(shuffleId)) {
				outstandingShuffles.remove(shuffleId);
				CancelTimeout ct = new CancelTimeout(shuffleId);
				trigger(ct, timer);
			}

			CyclonAddress peer = event.getCyclonSource();

			logger.debug("SHUFFLE_RESP from {}", peer);

			DescriptorBuffer receivedBuffer = event.getBuffer();
			cache.selectToKeep(peer, receivedBuffer.getDescriptors());
		}
	};

	/**
	 * Handles the timeout on a shuffle request. We don't have anything to do
	 * here, since we have already removed the (possibly dead) peer from out
	 * cache upon initiating the shuffle.
	 */
	Handler<ShuffleTimeout> handleShuffleTimeout = new Handler<ShuffleTimeout>() {
		public void handle(ShuffleTimeout event) {
			logger.warn("SHUFFLE TIMED OUT");
		}
	};
}
