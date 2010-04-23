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
package se.sics.kompics.p2p.overlay.chord;

import java.util.ArrayList;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.fd.FailureDetector;
import se.sics.kompics.p2p.overlay.chord.ring.ChordPS;
import se.sics.kompics.p2p.overlay.chord.ring.ChordPSInit;
import se.sics.kompics.p2p.overlay.chord.ring.ChordPeriodicStabilization;
import se.sics.kompics.p2p.overlay.chord.ring.NewPredecessor;
import se.sics.kompics.p2p.overlay.chord.ring.NewSuccessorList;
import se.sics.kompics.p2p.overlay.chord.router.ChordIterativeRouter;
import se.sics.kompics.p2p.overlay.chord.router.ChordIterativeRouterInit;
import se.sics.kompics.p2p.overlay.chord.router.ChordRouter;
import se.sics.kompics.timer.Timer;

/**
 * The <code>Chord</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class Chord extends ComponentDefinition {

	Negative<ChordStructuredOverlay> son = negative(ChordStructuredOverlay.class);
	Negative<ChordStatus> status = negative(ChordStatus.class);

	Positive<Network> network = positive(Network.class);
	Positive<Timer> timer = positive(Timer.class);
	Positive<FailureDetector> epfd = positive(FailureDetector.class);

	private Logger logger;
	Component ring;
	Component router;

	private int log2RingSize;
	private int successorListLength;
	private long stabilizationPeriod;
	private long fingerStabilizationPeriod;
	private long rpcTimeout;

	private boolean inside;
	private ChordAddress self;
	private ChordAddress predecessor;
	private ArrayList<ChordAddress> successorListView;
	private FingerTableView fingerTableView;
	private LinkedList<ChordLookupRequest> earlyLookupRequests;

	public Chord() {
		ring = create(ChordPS.class);
		router = create(ChordIterativeRouter.class);

		connect(network, ring.getNegative(Network.class));
		connect(network, router.getNegative(Network.class));

		connect(timer, ring.getNegative(Timer.class));
		connect(timer, router.getNegative(Timer.class));

		connect(epfd, ring.getNegative(FailureDetector.class));

		connect(ring.getNegative(ChordRouter.class), router
				.getPositive(ChordRouter.class));
		connect(router.getNegative(ChordPeriodicStabilization.class), ring
				.getPositive(ChordPeriodicStabilization.class));

		subscribe(handleInit, control);
		subscribe(handleCreate, son);
		subscribe(handleJoin, son);
		subscribe(handleJoinCompleted, ring
				.getPositive(ChordPeriodicStabilization.class));
		subscribe(handleLeave, son);
		subscribe(handleLeaveCompleted, ring
				.getPositive(ChordPeriodicStabilization.class));
		subscribe(handleLookupRequest, son);
		subscribe(handleLookupResponse, router.getPositive(ChordRouter.class));
		subscribe(handleResponsibilityRequest, son);
		subscribe(handleNeighborsRequest, status);
		subscribe(handleNewPredecessor, ring
				.getPositive(ChordPeriodicStabilization.class));
		subscribe(handleNewSuccessorList, ring
				.getPositive(ChordPeriodicStabilization.class));
		subscribe(handleNewFingerTable, router.getPositive(ChordRouter.class));

		earlyLookupRequests = new LinkedList<ChordLookupRequest>();
	}

	Handler<ChordInit> handleInit = new Handler<ChordInit>() {
		public void handle(ChordInit init) {
			inside = false;
			log2RingSize = init.getLog2RingSize();
			successorListLength = init.getSuccessorListLength();
			stabilizationPeriod = init.getStabilizationPeriod();
			fingerStabilizationPeriod = init.getFingerStabilizationPeriod();
			rpcTimeout = init.getRpcTimeout();
		}
	};

	Handler<CreateRing> handleCreate = new Handler<CreateRing>() {
		public void handle(CreateRing event) {
			if (inside) {
				logger.error("Already inside ring");
				return;
			}

			self = event.getSelf();

			// initialize ring
			trigger(new ChordPSInit(log2RingSize, successorListLength,
					stabilizationPeriod, self), ring.getControl());
			// initialize router
			trigger(new ChordIterativeRouterInit(log2RingSize,
					fingerStabilizationPeriod, rpcTimeout, self), router
					.getControl());
			logger = LoggerFactory.getLogger(getClass().getName() + "@"
					+ self.getKey());

			trigger(event, ring.getPositive(ChordPeriodicStabilization.class));
		}
	};

	Handler<JoinRing> handleJoin = new Handler<JoinRing>() {
		public void handle(JoinRing event) {
			if (inside) {
				logger.error("Already inside ring");
				return;
			}
			self = event.getSelf();

			// initialize ring
			trigger(new ChordPSInit(log2RingSize, successorListLength,
					stabilizationPeriod, self), ring.getControl());
			// initialize router
			trigger(new ChordIterativeRouterInit(log2RingSize,
					fingerStabilizationPeriod, rpcTimeout, self), router
					.getControl());
			logger = LoggerFactory.getLogger(getClass().getName() + "@"
					+ self.getKey());

			trigger(event, ring.getPositive(ChordPeriodicStabilization.class));
		}
	};

	Handler<JoinRingCompleted> handleJoinCompleted = new Handler<JoinRingCompleted>() {
		public void handle(JoinRingCompleted event) {
			inside = true;
			trigger(event, son);
		}
	};

	Handler<LeaveRing> handleLeave = new Handler<LeaveRing>() {
		public void handle(LeaveRing event) {
			if (!inside) {
				if (logger != null) {
					logger.error("Not inside ring");
				}
				return;
			}
			trigger(event, ring.getPositive(ChordPeriodicStabilization.class));
		}
	};

	Handler<LeaveRingCompleted> handleLeaveCompleted = new Handler<LeaveRingCompleted>() {
		public void handle(LeaveRingCompleted event) {
			inside = false;
			trigger(event, son);
		}
	};

	Handler<ChordLookupRequest> handleLookupRequest = new Handler<ChordLookupRequest>() {
		public void handle(ChordLookupRequest event) {
			if (inside) {
				trigger(event, router.getPositive(ChordRouter.class));
			} else {
				earlyLookupRequests.add(event);
			}
		}
	};

	Handler<ChordLookupResponse> handleLookupResponse = new Handler<ChordLookupResponse>() {
		public void handle(ChordLookupResponse event) {
			trigger(event, son);
		}
	};

	Handler<ChordResponsibilityRequest> handleResponsibilityRequest = new Handler<ChordResponsibilityRequest>() {
		public void handle(ChordResponsibilityRequest event) {
			ChordResponsibilityResponse response = new ChordResponsibilityResponse(
					event, predecessor.getKey(), self.getKey());
			trigger(response, son);
		}
	};

	Handler<ChordNeighborsRequest> handleNeighborsRequest = new Handler<ChordNeighborsRequest>() {
		public void handle(ChordNeighborsRequest event) {
			ChordNeighborsResponse response = new ChordNeighborsResponse(event,
					new ChordNeighbors(self, (successorListView == null ? self
							: successorListView.get(0)), predecessor,
							successorListView, fingerTableView));
			trigger(response, status);
		}
	};

	private Handler<NewSuccessorList> handleNewSuccessorList = new Handler<NewSuccessorList>() {
		public void handle(NewSuccessorList event) {
			logger.debug("NEW_SUCCESSOR_LIST {}", event.getSuccessorListView());

			successorListView = event.getSuccessorListView();
		}
	};

	private Handler<NewPredecessor> handleNewPredecessor = new Handler<NewPredecessor>() {
		public void handle(NewPredecessor event) {
			logger.debug("NEW_PREDECESSOR {}", event.getPredecessorPeer());

			if (predecessor != null && event.getPredecessorPeer() != null) {

				if (!predecessor.equals(event.getPredecessorPeer())) {
					predecessor = event.getPredecessorPeer();

					trigger(new NewChordResponsibility(predecessor.getKey(),
							self.getKey()), son);
				}
			} else if (event.getPredecessorPeer() != null
					&& predecessor == null) {
				predecessor = event.getPredecessorPeer();
				trigger(new NewChordResponsibility(predecessor.getKey(), self
						.getKey()), son);
			}
		}
	};

	private Handler<NewFingerTable> handleNewFingerTable = new Handler<NewFingerTable>() {
		public void handle(NewFingerTable event) {
			logger.debug("NEW_FINGER_TABLE {}", event.getFingerTableView());
			fingerTableView = event.getFingerTableView();

			if (inside && !earlyLookupRequests.isEmpty()) {
				for (ChordLookupRequest request : earlyLookupRequests) {
					trigger(request, router.getPositive(ChordRouter.class));
				}
				earlyLookupRequests.clear();
			}
			
			trigger(event, son);
		}
	};
}
