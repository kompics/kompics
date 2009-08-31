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
package se.sics.kompics.p2p.overlay.chord.router;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.overlay.chord.ChordAddress;
import se.sics.kompics.p2p.overlay.chord.ChordLookupRequest;
import se.sics.kompics.p2p.overlay.chord.ChordLookupResponse;
import se.sics.kompics.p2p.overlay.chord.LookupInfo;
import se.sics.kompics.p2p.overlay.chord.ChordLookupResponse.ChordLookupStatus;
import se.sics.kompics.p2p.overlay.chord.ring.ChordPeriodicStabilization;
import se.sics.kompics.p2p.overlay.chord.ring.NewPredecessor;
import se.sics.kompics.p2p.overlay.chord.ring.NewSuccessorList;
import se.sics.kompics.p2p.overlay.key.NumericRingKey;
import se.sics.kompics.p2p.overlay.key.RingKey.IntervalBounds;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

/**
 * The <code>ChordIterativeRouter</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class ChordIterativeRouter extends ComponentDefinition {

	Negative<ChordRouter> router = negative(ChordRouter.class);

	Positive<ChordPeriodicStabilization> ps = positive(ChordPeriodicStabilization.class);
	Positive<Network> network = positive(Network.class);
	Positive<Timer> timer = positive(Timer.class);

	private Logger logger;
	private int log2RingSize;
	private long fingerStabilizationPeriod;
	private long rpcTimeout;
	private BigInteger ringSize;
	private ChordAddress self, predecessor, successor;
	private FingerTable fingerTable;
	private HashMap<UUID, LookupInfo> outstandingLookups;
	private final HashSet<UUID> outstandingTimeouts;
	private ChordIterativeRouter thisRouter;

	public ChordIterativeRouter() {
		thisRouter = this;
		outstandingLookups = new HashMap<UUID, LookupInfo>();
		outstandingTimeouts = new HashSet<UUID>();

		subscribe(handleInit, control);

		subscribe(handleFindSuccessorRequest, network);
		subscribe(handleFindSuccessorResponse, network);
		subscribe(handleGetFingerTableRequest, network);
		subscribe(handleGetFingerTableResponse, network);

		subscribe(handleChordLookupRequest, router);

		subscribe(handleNewSuccessorList, ps);
		subscribe(handleNewPredecessor, ps);

		subscribe(handleRpcTimeout, timer);
		subscribe(handleFixFingersTimeout, timer);
	}

	Handler<ChordIterativeRouterInit> handleInit = new Handler<ChordIterativeRouterInit>() {
		public void handle(ChordIterativeRouterInit init) {
			fingerStabilizationPeriod = init.getFingerStabilizationPeriod();
			log2RingSize = init.getLog2RingSize();
			rpcTimeout = init.getRpcTimeout();

			ringSize = new BigInteger("2").pow(log2RingSize);
			self = init.getSelf();

			fingerTable = new FingerTable(log2RingSize, self, thisRouter);
			fingerTableChanged();

			logger = LoggerFactory.getLogger(getClass().getName() + "@"
					+ self.getKey().getId());
		}
	};

	private Handler<NewSuccessorList> handleNewSuccessorList = new Handler<NewSuccessorList>() {
		public void handle(NewSuccessorList event) {
			logger.debug("NEW_SUCC {}", event.getSuccessorListView());

			if (successor == null) {
				// if this is the first time we get this event, we initialize
				// the periodic FixFingers timer
				SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(
						fingerStabilizationPeriod, fingerStabilizationPeriod);
				spt.setTimeoutEvent(new FixFingersTimeout(spt));
				trigger(spt, timer);

				successor = event.getSuccessorListView().get(0);

				if (!successor.equals(self)) {
					// ask successor for its finger table
					GetFingerTableRequest request = new GetFingerTableRequest(
							self, successor);
					trigger(request, network);
				}
			}

			successor = event.getSuccessorListView().get(0);

			boolean changed = false;
			// try to use the successors
			for (ChordAddress address : event.getSuccessorListView()) {
				if (fingerTable.learnedAboutPeer(address, false, false)) {
					changed = true;
				}
			}
			if (changed) {
				fingerTableChanged();
			}
		}
	};

	private Handler<NewPredecessor> handleNewPredecessor = new Handler<NewPredecessor>() {
		public void handle(NewPredecessor event) {
			logger.debug("NEW_PRED {}", event.getPredecessorPeer());

			predecessor = event.getPredecessorPeer();
		}
	};

	private Handler<ChordLookupRequest> handleChordLookupRequest = new Handler<ChordLookupRequest>() {
		public void handle(ChordLookupRequest event) {
			ChordLookupResponse response = doLookup(event, false);
			if (response != null) {
				trigger(response, router);
			}
		}
	};

	private final ChordLookupResponse doLookup(ChordLookupRequest event,
			boolean maintenance) {
		logger.debug("CHORD_LOOKUP_REQ({})", event.getKey());

		NumericRingKey key = event.getKey();
		ChordAddress firstPeer = event.getFirstPeer();

		// special case for Ring join, we don't have a successor yet.
		if (firstPeer != null) {

			logger.debug("FIRST_PEER is {}", firstPeer);

			LookupInfo lookupInfo = new LookupInfo(event);
			lookupInfo.initiatedNow();

			ScheduleTimeout st = new ScheduleTimeout(rpcTimeout);
			st
					.setTimeoutEvent(new RpcTimeout(st, event, firstPeer,
							maintenance));
			UUID timeoutId = st.getTimeoutEvent().getTimeoutId();
			outstandingLookups.put(timeoutId, lookupInfo);
			outstandingTimeouts.add(timeoutId);
			trigger(st, timer);

			FindSuccessorRequest request = new FindSuccessorRequest(key,
					timeoutId, self, firstPeer, maintenance);

			trigger(request, network);

			// we try to use the hinted first peer as a possible better
			// finger
			fingerTable.learnedAboutFreshPeer(firstPeer);
			return null;
		}

		// special case for when we are alone in the ring
		if (successor == null || successor.equals(self)) {
			// to avoid an infinite loop, we return ourselves
			return new ChordLookupResponse(event, key, self, event
					.getAttachment(), new LookupInfo(event));
		}

		// normal case
		if (predecessor != null
				&& key.belongsTo(predecessor.getKey(), self.getKey(),
						IntervalBounds.OPEN_CLOSED, ringSize)) {
			// we are responsible
			return new ChordLookupResponse(event, key, self, event
					.getAttachment(), new LookupInfo(event));
		} else if (key.belongsTo(self.getKey(), successor.getKey(),
				IntervalBounds.OPEN_CLOSED, ringSize)) {
			// our successor is responsible for the looked up key
			return new ChordLookupResponse(event, key, successor, event
					.getAttachment(), new LookupInfo(event));
		} else {
			// some other peer is responsible for the looked up key
			LookupInfo lookupInfo = new LookupInfo(event);
			lookupInfo.initiatedNow();

			ChordAddress closest = fingerTable.closestPreceedingPeer(key);

			if (closest.equals(self)) {
				// we found no closest peer so the lookup should fail
				return new ChordLookupResponse(event, key, event
						.getAttachment(), null);
			}

			ScheduleTimeout st = new ScheduleTimeout(rpcTimeout);
			st.setTimeoutEvent(new RpcTimeout(st, event, closest, maintenance));
			UUID timeoutId = st.getTimeoutEvent().getTimeoutId();
			outstandingLookups.put(timeoutId, lookupInfo);
			outstandingTimeouts.add(timeoutId);
			trigger(st, timer);

			FindSuccessorRequest request = new FindSuccessorRequest(key,
					timeoutId, self, closest, maintenance);

			trigger(request, network);
			return null;
		}
	}

	private Handler<FindSuccessorRequest> handleFindSuccessorRequest = new Handler<FindSuccessorRequest>() {
		public void handle(FindSuccessorRequest event) {
			logger
					.debug("FIND_SUCC_REQ@{} for key {},succ={},pred={}",
							new Object[] { self, event.getKey(), successor,
									predecessor });

			NumericRingKey key = event.getKey();

			if (predecessor != null
					&& key.belongsTo(predecessor.getKey(), self.getKey(),
							IntervalBounds.OPEN_CLOSED, ringSize)) {
				// return ourselves as the real responsible
				FindSuccessorResponse response = new FindSuccessorResponse(
						self, event.getLookupId(), false, self, event
								.getChordSource(), event.isMaintenance());
				trigger(response, network);
			} else if (successor != null) {
				if (key.belongsTo(self.getKey(), successor.getKey(),
						IntervalBounds.OPEN_CLOSED, ringSize)) {
					// return my successor as the real successor
					FindSuccessorResponse response = new FindSuccessorResponse(
							successor, event.getLookupId(), false, self, event
									.getChordSource(), event.isMaintenance());
					trigger(response, network);
				} else {
					// return an indirection to my closest preceding finger
					ChordAddress closest = fingerTable
							.closestPreceedingPeer(key);

					if (!closest.equals(self)) {
						FindSuccessorResponse response = new FindSuccessorResponse(
								closest, event.getLookupId(), true, self, event
										.getChordSource(), event
										.isMaintenance());
						trigger(response, network);
					}
					// else we found no closest peer so the lookup should fail
				}
			}

			// we try to use the requester as a possible better finger
			fingerTable.learnedAboutFreshPeer(event.getChordSource());
		}
	};

	private Handler<FindSuccessorResponse> handleFindSuccessorResponse = new Handler<FindSuccessorResponse>() {
		public void handle(FindSuccessorResponse event) {
			UUID lookupId = event.getLookupId();
			if (outstandingTimeouts.contains(lookupId)) {
				// we got the response before the RpcTimeout, so we cancel the
				// timer
				trigger(new CancelTimeout(lookupId), timer);
				outstandingTimeouts.remove(lookupId);
			} else {
				// we got the response too late so we just give up since we have
				// already triggered a LookupFailed event
				return;
			}

			LookupInfo lookupInfo = outstandingLookups.remove(lookupId);
			ChordLookupRequest lookupRequest = lookupInfo.getRequest();

			logger.debug("FIND_SUCC_RESP NH({}) is {}. {}", new Object[] {
					lookupRequest.getKey(), event.getSuccessor(),
					event.isNextHop() });

			if (event.isNextHop()) {
				// we got an indirection
				ChordAddress nextHop = event.getSuccessor();

				ScheduleTimeout st = new ScheduleTimeout(rpcTimeout);
				st.setTimeoutEvent(new RpcTimeout(st, lookupRequest, nextHop,
						event.isMaintenance()));
				UUID timeoutId = st.getTimeoutEvent().getTimeoutId();
				lookupInfo.appendHop(event.getChordSource());
				outstandingLookups.put(timeoutId, lookupInfo);
				outstandingTimeouts.add(timeoutId);
				trigger(st, timer);

				FindSuccessorRequest request = new FindSuccessorRequest(
						lookupRequest.getKey(), timeoutId, self, nextHop, event
								.isMaintenance());

				// send find successor request
				trigger(request, network);
			} else {
				// we got the real responsible
				lookupInfo.appendHop(event.getChordSource());
				lookupInfo.completedNow();
				ChordLookupResponse response = new ChordLookupResponse(
						lookupRequest, lookupRequest.getKey(), event
								.getSuccessor(), lookupRequest.getAttachment(),
						lookupInfo);

				if (event.isMaintenance()) {
					ChordAddress fingerPeer = response.getResponsible();
					int fingerIndex = (Integer) response.getAttachment();
					fingerTable.fingerFixed(fingerIndex, fingerPeer);
				} else {
					// System.err.println(response.getLookupInfo());
					trigger(response, router);
				}
			}

			// we try to use the responsible or the next hop as a possible
			// better finger
			fingerTable.learnedAboutPeer(event.getSuccessor());
		}
	};

	private Handler<RpcTimeout> handleRpcTimeout = new Handler<RpcTimeout>() {
		public void handle(RpcTimeout event) {
			logger.debug("RPC_TIMEOUT");

			UUID timeoutId = event.getTimeoutId();

			if (outstandingTimeouts.contains(timeoutId)) {
				outstandingTimeouts.remove(timeoutId);

				// we got an RPC timeout before the RPC response so we have to
				// return a ChordLookupFailed event and to mark the finger as
				// failed
				ChordLookupRequest lookupRequest = outstandingLookups.remove(
						timeoutId).getRequest();
				ChordLookupResponse failed = new ChordLookupResponse(
						lookupRequest, lookupRequest.getKey(), lookupRequest
								.getAttachment(), event.getPeer());

				if (!event.isMaintenance()) {
					trigger(failed, router);
				}

				// mark the slow/failed peer as suspected
				fingerTable.fingerSuspected(event.getPeer());
			}
			// else we got the RPC response before the timeout so everything is
			// OK.
		}
	};

	private Handler<FixFingersTimeout> handleFixFingersTimeout = new Handler<FixFingersTimeout>() {
		public void handle(FixFingersTimeout event) {
			logger.debug("FIX_FINGER");

			int nextFingerToFix = fingerTable.nextFingerToFix();

			// actually fix the finger now
			// finger[next] = findSuccessor(n plus 2^(next-1))
			NumericRingKey fingerBegin = fingerTable
					.getFingerBegin(nextFingerToFix);

			ChordLookupRequest request = new ChordLookupRequest(fingerBegin,
					nextFingerToFix);

			// here I need to use myself
			ChordLookupResponse response = doLookup(request, true);
			if (response != null) {
				if (response.getStatus() == ChordLookupStatus.FAILURE) {
					logger.debug("CHORD_LOOKUP_FAILED");
					ChordAddress fingerPeer = response.getResponsible();
					fingerTable.fingerSuspected(fingerPeer);
				} else {
					logger.debug("CHORD_LOOKUP_RESP");

					ChordAddress fingerPeer = response.getResponsible();
					int fingerIndex = (Integer) response.getAttachment();
					fingerTable.fingerFixed(fingerIndex, fingerPeer);
				}
			}
		}
	};

	private Handler<GetFingerTableRequest> handleGetFingerTableRequest = new Handler<GetFingerTableRequest>() {
		public void handle(GetFingerTableRequest event) {
			logger.debug("GET_FINGER_TABLE_REQ");

			GetFingerTableResponse response = new GetFingerTableResponse(
					fingerTable.getView(), self, event.getChordSource());
			trigger(response, network);
		}
	};

	private Handler<GetFingerTableResponse> handleGetFingerTableResponse = new Handler<GetFingerTableResponse>() {
		public void handle(GetFingerTableResponse event) {
			logger.debug("GET_FINGER_TABLE_RESP");

			// we try to populate our finger table with our successor's finger
			// table
			boolean changed = false;
			// try to use the successors
			for (ChordAddress address : event.getFingerTable().finger) {
				if (fingerTable.learnedAboutPeer(address, false, false)) {
					changed = true;
				}
			}
			if (changed) {
				fingerTableChanged();
			}
			// trigger(new JoinRingCompleted(localPeer),
			// notificationChannel);
		}
	};

	final void fingerTableChanged() {
		trigger(new NewFingerTable(fingerTable.getView()), router);
	}
}
