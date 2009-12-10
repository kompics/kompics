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
package se.sics.kompics.p2p.fd.ping;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.p2p.fd.FailureDetector;
import se.sics.kompics.p2p.fd.PeerFailureSuspicion;
import se.sics.kompics.p2p.fd.StartProbingPeer;
import se.sics.kompics.p2p.fd.StopProbingPeer;
import se.sics.kompics.p2p.fdstatus.FailureDetectorStatus;
import se.sics.kompics.p2p.fdstatus.ProbedPeerData;
import se.sics.kompics.p2p.fdstatus.StatusRequest;
import se.sics.kompics.p2p.fdstatus.StatusResponse;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

/**
 * The <code>PingFailureDetector</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class PingFailureDetector extends ComponentDefinition {

	Negative<FailureDetector> fd = negative(FailureDetector.class);
	Negative<FailureDetectorStatus> fdControl = negative(FailureDetectorStatus.class);

	Positive<Timer> timer = positive(Timer.class);
	Positive<Network> net = positive(Network.class);

	Logger logger;

	private final HashSet<UUID> outstandingTimeouts;

	private HashMap<Address, PeerProber> peerProbers;

	long minRto, livePingInterval, deadPingInterval, pongTimeoutIncrement;

	private Address self;

	private final PingFailureDetector thisFd;

	private Transport protocol;

	public PingFailureDetector() {
		peerProbers = new HashMap<Address, PeerProber>();
		this.outstandingTimeouts = new HashSet<UUID>();
		this.thisFd = this;

		subscribe(handleInit, control);

		subscribe(handleSendPing, timer);
		subscribe(handlePongTimedOut, timer);

		subscribe(handlePing, net);
		subscribe(handlePong, net);

		subscribe(handleStartProbingPeer, fd);
		subscribe(handleStopProbingPeer, fd);

		subscribe(handleStatusRequest, fdControl);
	}

	private Handler<PingFailureDetectorInit> handleInit = new Handler<PingFailureDetectorInit>() {
		public void handle(PingFailureDetectorInit event) {
			minRto = event.getConfiguration().getMinRto();
			livePingInterval = event.getConfiguration().getLivePeriod();
			deadPingInterval = event.getConfiguration().getSuspectedPeriod();
			pongTimeoutIncrement = event.getConfiguration()
					.getTimeoutPeriodIncrement();
			protocol = event.getConfiguration().getProtocol();

			self = event.getSelf();
			logger = LoggerFactory.getLogger(PingFailureDetector.class
					.getName()
					+ "@" + self.getId());
		};
	};

	private Handler<StartProbingPeer> handleStartProbingPeer = new Handler<StartProbingPeer>() {
		public void handle(StartProbingPeer event) {
			Address peerAddress = event.getPeerAddress();
			PeerProber peerProber = peerProbers.get(peerAddress);
			if (peerProber == null) {
				peerProber = new PeerProber(peerAddress, thisFd);
				peerProber.addRequest(event);
				peerProbers.put(peerAddress, peerProber);
				peerProber.start();
				logger.debug("Started probing peer {}", peerAddress);
			} else {
				peerProber.addRequest(event);
				logger.debug("Peer {} is already being probed", peerAddress);
			}
		}
	};

	private Handler<StopProbingPeer> handleStopProbingPeer = new Handler<StopProbingPeer>() {
		public void handle(StopProbingPeer event) {
			Address peerAddress = event.getPeerAddress();
			PeerProber prober = peerProbers.get(peerAddress);
			if (prober != null) {
				UUID requestId = event.getRequestId();
				if (prober.hasRequest(requestId)) {
					boolean last = prober.removeRequest(requestId);
					if (last) {
						peerProbers.remove(peerAddress);
						prober.stop();
						logger.debug("Stoped probing peer {}", peerAddress);
					}
				} else {
					logger.debug(
							"I have no request {} for the probing of peer {}",
							requestId, peerAddress);
				}
			} else {
				logger.debug("Peer {} is not currently being probed",
						peerAddress);
			}
		}
	};

	private Handler<SendPing> handleSendPing = new Handler<SendPing>() {
		public void handle(SendPing event) {
			Address peer = event.getPeer();
			PeerProber prober = peerProbers.get(peer);
			if (prober != null) {
				prober.ping();
			} else {
				logger.debug("Peer {} is not currently being probed", peer);
			}
		}
	};

	private Handler<PongTimedOut> handlePongTimedOut = new Handler<PongTimedOut>() {
		public void handle(PongTimedOut event) {
			if (outstandingTimeouts.contains(event.getTimeoutId())) {
				Address peer = event.getPeer();
				PeerProber peerProber = peerProbers.get(peer);
				outstandingTimeouts.remove(event.getTimeoutId());
				if (peerProber != null) {
					peerProber.pongTimedOut();
				} else {
					logger.debug("Peer {} is not currently being probed", peer);
				}
			}
		}
	};

	private Handler<Ping> handlePing = new Handler<Ping>() {
		public void handle(Ping event) {
			logger.debug("Received Ping from {}. Sending Pong. {}", event
					.getSource(), event.getId());
			trigger(new Pong(event.getId(), event.getTs(), self, event
					.getSource(), protocol), net);
		}
	};

	private Handler<Pong> handlePong = new Handler<Pong>() {
		public void handle(Pong event) {
			if (outstandingTimeouts.remove(event.getId())) {
				trigger(new CancelTimeout(event.getId()), timer);
			}
			Address peer = event.getSource();
			PeerProber peerProber = peerProbers.get(peer);

			if (peerProber != null) {
				peerProber.pong(event.getId(), event.getTs());
			} else {
				logger.debug("Peer {} is not currently being probed", peer);
			}
		}
	};

	private Handler<StatusRequest> handleStatusRequest = new Handler<StatusRequest>() {
		public void handle(StatusRequest request) {
			Map<Address, ProbedPeerData> probedPeers = new HashMap<Address, ProbedPeerData>();
			for (Map.Entry<Address, PeerProber> entry : peerProbers.entrySet()) {
				probedPeers.put(entry.getKey(), entry.getValue()
						.getProbedPeerData());
			}
			trigger(new StatusResponse(request, probedPeers), fd);
		}
	};

	void stop(UUID intervalPingTimerId, UUID pongTimeoutId) {
		if (outstandingTimeouts.remove(intervalPingTimerId)) {
			trigger(new CancelTimeout(intervalPingTimerId), timer);
		}
		if (outstandingTimeouts.remove(pongTimeoutId)) {
			trigger(new CancelTimeout(pongTimeoutId), timer);
		}
	}

	UUID sendPing(long ts, Address probedPeer, long expectedRtt) {
		// Setting timer for the receiving the Pong packet
		ScheduleTimeout st = new ScheduleTimeout(expectedRtt
				+ pongTimeoutIncrement);
		st.setTimeoutEvent(new PongTimedOut(st, probedPeer));
		UUID pongTimeoutId = st.getTimeoutEvent().getTimeoutId();
		outstandingTimeouts.add(pongTimeoutId);

		trigger(st, timer);
		trigger(new Ping(pongTimeoutId, ts, self, probedPeer, protocol), net);
		return pongTimeoutId;
	}

	UUID setPingTimer(boolean suspected, Address probedPeer) {
		long interval = suspected ? deadPingInterval : livePingInterval;
		ScheduleTimeout st = new ScheduleTimeout(interval);
		st.setTimeoutEvent(new SendPing(st, probedPeer));
		UUID intervalPingTimeoutId = st.getTimeoutEvent().getTimeoutId();

		// we must not add this timeout id to outstandingTimeout!
		
		trigger(st, timer);
		return intervalPingTimeoutId;
	}

	void suspect(PeerFailureSuspicion suspectEvent) {
		trigger(suspectEvent, fd);
	}

	void revise(PeerFailureSuspicion reviseEvent) {
		trigger(reviseEvent, fd);
	}
}
