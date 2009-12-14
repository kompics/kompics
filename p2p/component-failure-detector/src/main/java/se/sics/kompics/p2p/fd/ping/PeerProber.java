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
import java.util.Set;
import java.util.UUID;

import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.fd.PeerFailureSuspicion;
import se.sics.kompics.p2p.fd.StartProbingPeer;
import se.sics.kompics.p2p.fd.SuspicionStatus;
import se.sics.kompics.p2p.fdstatus.ProbedPeerData;
import se.sics.kompics.p2p.overlay.OverlayAddress;

/**
 * The <code>PeerProber</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class PeerProber {
//	private static Logger logger = LoggerFactory.getLogger(PeerProber.class);

	private UUID intervalPingTimeoutId;
	private UUID pongTimeoutId;

	private boolean suspected;

	private PingFailureDetector fd;
	private OverlayAddress firstRequest;

	private PeerResponseTime times;
	private Set<UUID> requestIds;
	private HashMap<UUID, StartProbingPeer> requests;

	private Address probedPeer;

	PeerProber(Address probedPeer, PingFailureDetector fd) {
		this.probedPeer = probedPeer;
		this.fd = fd;

		suspected = false;

		this.requestIds = new HashSet<UUID>();
		requests = new HashMap<UUID, StartProbingPeer>();

		this.times = new PeerResponseTime(fd.minRto);
	}

	void start() {
		intervalPingTimeoutId = fd.setPingTimer(suspected, probedPeer);
	}

	void ping() {
		intervalPingTimeoutId = fd.setPingTimer(suspected, probedPeer);
		pongTimeoutId = fd.sendPing(System.currentTimeMillis(), probedPeer,
				times.getRTO());
//		logger.debug("@{}: PING {}", pongTimeoutId);
	}

	void pong(UUID pongId, long ts) {
		long RTT = System.currentTimeMillis() - ts;
		times.updateRTO(RTT);

//		logger.debug("@{}: PoNG {} RTT={}", pongId, RTT);

		if (suspected == true) {
			suspected = false;
			reviseSuspicion();
		}
	}

	void pongTimedOut() {
		if (suspected == false) {
			suspected = true;
			times.timedOut();
			suspect();
		}
	}

	private void suspect() {
//		logger.debug("Peer {} is suspected", probedPeer);
		for (StartProbingPeer req : requests.values()) {
			PeerFailureSuspicion commPeerSuspectedEvent = new PeerFailureSuspicion(
					req, probedPeer, req.getOverlayAddress(),
					SuspicionStatus.SUSPECTED);
			fd.suspect(commPeerSuspectedEvent);
		}
	}

	private void reviseSuspicion() {
		// Revising previous suspicion
		for (StartProbingPeer req : requests.values()) {
			PeerFailureSuspicion commRectifyEvent = new PeerFailureSuspicion(
					req, probedPeer, req.getOverlayAddress(),
					SuspicionStatus.ALIVE);
			fd.revise(commRectifyEvent);
		}
	}

	void stop() {
		fd.stop(intervalPingTimeoutId, pongTimeoutId);
	}

	void addRequest(StartProbingPeer request) {
		requestIds.add(request.getRequestId());
		requests.put(request.getRequestId(), request);
		if (firstRequest == null) {
			firstRequest = request.getOverlayAddress();
		}
	}

	boolean removeRequest(UUID requestId) {
		requestIds.remove(requestId);
		requests.remove(requestId);
		return requestIds.isEmpty();
	}

	boolean hasRequest(UUID requestId) {
		return requestIds.contains(requestId);
	}

	ProbedPeerData getProbedPeerData() {
		return times.getProbedPeerData(firstRequest);
	}
}
