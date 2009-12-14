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
package se.sics.kompics.p2p.bootstrap.client;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.bootstrap.BootstrapCacheReset;
import se.sics.kompics.p2p.bootstrap.BootstrapCompleted;
import se.sics.kompics.p2p.bootstrap.BootstrapRequest;
import se.sics.kompics.p2p.bootstrap.BootstrapResponse;
import se.sics.kompics.p2p.bootstrap.P2pBootstrap;
import se.sics.kompics.p2p.bootstrap.PeerEntry;
import se.sics.kompics.p2p.bootstrap.server.CacheAddPeerRequest;
import se.sics.kompics.p2p.bootstrap.server.CacheGetPeersRequest;
import se.sics.kompics.p2p.bootstrap.server.CacheGetPeersResponse;
import se.sics.kompics.p2p.bootstrap.server.CacheResetRequest;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

/**
 * The <code>BootstrapClient</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class BootstrapClient extends ComponentDefinition {

	Negative<P2pBootstrap> bootstrap = negative(P2pBootstrap.class);

	Positive<Network> network = positive(Network.class);
	Positive<Timer> timer = positive(Timer.class);

	private static Logger logger = LoggerFactory
			.getLogger(BootstrapClient.class);

	private final HashSet<UUID> outstandingTimeouts;
	private BootstrapRequest activeBootstrapRequest;

	private HashSet<PeerEntry> overlays;

	private Address bootstrapServerAddress;
	private Address self;
	private int clientWebPort;
	private long refreshPeriod;
	private long retryPeriod;
	private int retriesCount;

	public BootstrapClient() {
		outstandingTimeouts = new HashSet<UUID>();
		overlays = new HashSet<PeerEntry>();

		subscribe(handleInit, control);

		subscribe(handleBootstrapRequest, bootstrap);
		subscribe(handleBootstrapCompleted, bootstrap);
		subscribe(handleBootstrapCacheReset, bootstrap);
		subscribe(handleCacheGetPeersResponse, network);
		subscribe(handleClientRefreshPeer, timer);
		subscribe(handleClientRetryRequest, timer);
	}

	Handler<BootstrapClientInit> handleInit = new Handler<BootstrapClientInit>() {
		public void handle(BootstrapClientInit init) {
			refreshPeriod = init.getBootstrapConfiguration()
					.getClientKeepAlivePeriod();
			retryPeriod = init.getBootstrapConfiguration()
					.getClientRetryPeriod();
			retriesCount = init.getBootstrapConfiguration()
					.getClientRetryCount();
			bootstrapServerAddress = init.getBootstrapConfiguration()
					.getBootstrapServerAddress();
			clientWebPort = init.getBootstrapConfiguration().getClientWebPort();
			self = init.getSelf();
		}
	};

	private Handler<BootstrapRequest> handleBootstrapRequest = new Handler<BootstrapRequest>() {
		public void handle(BootstrapRequest event) {
			// set an alarm to retry the request if no response
			ScheduleTimeout st = new ScheduleTimeout(retryPeriod);
			ClientRetryRequest retryRequest = new ClientRetryRequest(st,
					retriesCount, event);
			st.setTimeoutEvent(retryRequest);
			UUID timerId = retryRequest.getTimeoutId();
			CacheGetPeersRequest request = new CacheGetPeersRequest(event
					.getOverlay(), event.getPeersMax(), timerId, self,
					bootstrapServerAddress);

			outstandingTimeouts.add(timerId);

			activeBootstrapRequest = event;

			trigger(request, network);
			trigger(st, timer);

			logger.debug("@{}: Sending GetPeersRequest to ", self.getId(),
					bootstrapServerAddress);
		}
	};

	private Handler<ClientRetryRequest> handleClientRetryRequest = new Handler<ClientRetryRequest>() {
		public void handle(ClientRetryRequest event) {
			if (!outstandingTimeouts.contains(event.getTimeoutId())) {
				return;
			}
			outstandingTimeouts.remove(event.getTimeoutId());

			if (event.getRetriesLeft() > 0) {
				// set an alarm to retry the request if no response
				ScheduleTimeout st = new ScheduleTimeout(retryPeriod);
				ClientRetryRequest retryRequest = new ClientRetryRequest(st,
						event.getRetriesLeft() - 1, event.getRequest());
				st.setTimeoutEvent(retryRequest);
				UUID timerId = retryRequest.getTimeoutId();
				CacheGetPeersRequest request = new CacheGetPeersRequest(event
						.getRequest().getOverlay(), event.getRequest()
						.getPeersMax(), timerId, self, bootstrapServerAddress);

				outstandingTimeouts.add(timerId);

				activeBootstrapRequest = event.getRequest();

				trigger(request, network);
				trigger(st, timer);

				logger.debug("@{}: Sending GetPeersRequest to  ", self.getId(),
						bootstrapServerAddress);
			} else {
				BootstrapResponse response = new BootstrapResponse(
						activeBootstrapRequest, false, activeBootstrapRequest
								.getOverlay(), null);
				trigger(response, bootstrap);
			}
		}
	};

	private Handler<CacheGetPeersResponse> handleCacheGetPeersResponse = new Handler<CacheGetPeersResponse>() {
		public void handle(CacheGetPeersResponse event) {
			if (outstandingTimeouts.contains(event.getRequestId())) {
				CancelTimeout ct = new CancelTimeout(event.getRequestId());
				trigger(ct, timer);
				outstandingTimeouts.remove(event.getRequestId());
			} else {
				// request was retried. we ignore this first slow response.
				// (to avoid double response;TODO add a local BOOTSTRAPPED flag
				// per overlay)
				return;
			}

			// TODO request map for MULTIPLE overlays
			BootstrapResponse response = new BootstrapResponse(
					activeBootstrapRequest, true, activeBootstrapRequest
							.getOverlay(), event.getPeers());

			logger.debug("@{}: Received GetPeersResponse {}", self.getId(),
					event.getPeers());
			trigger(response, bootstrap);
		}
	};

	@SuppressWarnings("unchecked")
	private Handler<BootstrapCompleted> handleBootstrapCompleted = new Handler<BootstrapCompleted>() {
		public void handle(BootstrapCompleted event) {
			PeerEntry peerEntry = new PeerEntry(event.getOverlay(), event
					.getOverlayAddress(), clientWebPort, self, 0, 0);

			overlays.add(peerEntry);

			CacheAddPeerRequest request = new CacheAddPeerRequest(self,
					bootstrapServerAddress, (Set<PeerEntry>) overlays.clone());

			trigger(request, network);

			// set refresh periodic timer
			ScheduleTimeout st = new ScheduleTimeout(refreshPeriod);
			st.setTimeoutEvent(new ClientRefreshPeer(st));
			trigger(st, timer);
		}
	};

	@SuppressWarnings("unchecked")
	private Handler<ClientRefreshPeer> handleClientRefreshPeer = new Handler<ClientRefreshPeer>() {
		public void handle(ClientRefreshPeer event) {
			CacheAddPeerRequest request = new CacheAddPeerRequest(self,
					bootstrapServerAddress, (Set<PeerEntry>) overlays.clone());
			trigger(request, network);
		}
	};

	private Handler<BootstrapCacheReset> handleBootstrapCacheReset = new Handler<BootstrapCacheReset>() {
		public void handle(BootstrapCacheReset event) {
			CacheResetRequest request = new CacheResetRequest(self,
					bootstrapServerAddress, event.getOverlay());
			trigger(request, network);
		}
	};
}
