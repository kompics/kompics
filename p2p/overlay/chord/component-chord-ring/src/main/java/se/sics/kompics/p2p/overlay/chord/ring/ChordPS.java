/**
 * This file is part of the Kompics P2P Framework.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.kompics.p2p.overlay.chord.ring;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.fd.FailureDetector;
import se.sics.kompics.p2p.fd.PeerFailureSuspicion;
import se.sics.kompics.p2p.fd.StartProbingPeer;
import se.sics.kompics.p2p.fd.StopProbingPeer;
import se.sics.kompics.p2p.fd.SuspicionStatus;
import se.sics.kompics.p2p.overlay.chord.ChordAddress;
import se.sics.kompics.p2p.overlay.chord.ChordLookupRequest;
import se.sics.kompics.p2p.overlay.chord.ChordLookupResponse;
import se.sics.kompics.p2p.overlay.chord.CreateRing;
import se.sics.kompics.p2p.overlay.chord.JoinRing;
import se.sics.kompics.p2p.overlay.chord.JoinRingCompleted;
import se.sics.kompics.p2p.overlay.chord.LeaveRing;
import se.sics.kompics.p2p.overlay.chord.LeaveRingCompleted;
import se.sics.kompics.p2p.overlay.chord.ChordLookupResponse.ChordLookupStatus;
import se.sics.kompics.p2p.overlay.chord.router.ChordRouter;
import se.sics.kompics.p2p.overlay.key.RingKey.IntervalBounds;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;

/**
 * The
 * <code>ChordPS</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class ChordPS extends ComponentDefinition {

    Negative<ChordPeriodicStabilization> ps = negative(ChordPeriodicStabilization.class);
    Positive<ChordRouter> router = positive(ChordRouter.class);
    Positive<Network> network = positive(Network.class);
    Positive<Timer> timer = positive(Timer.class);
    Positive<FailureDetector> epfd = positive(FailureDetector.class);
    private Logger logger;
    private long stabilizationPeriod;
    private BigInteger ringSize;
    private ChordAddress self, predecessor;
    private SuccessorList successorList;
    private HashMap<Address, ChordAddress> neighborPeers;
    private HashMap<Address, Integer> neighborCounters;
    private HashMap<Address, UUID> fdRequests;

    public ChordPS(ChordPSInit init) {
        neighborPeers = new HashMap<Address, ChordAddress>();
        neighborCounters = new HashMap<Address, Integer>();
        fdRequests = new HashMap<Address, UUID>();



        subscribe(handleCreateRing, ps);
        subscribe(handleJoinRing, ps);
        subscribe(handleLeaveRing, ps);
        subscribe(handleChordLookupResponse, router);
        subscribe(handleGetPredecessorRequest, network);
        subscribe(handleGetPredecessorResponse, network);
        subscribe(handleGetSuccessorListRequest, network);
        subscribe(handleGetSuccessorListResponse, network);
        subscribe(handleLeaveNotification, network);
        subscribe(handleNotify, network);
        subscribe(handleStabilizeTimeout, timer);
        subscribe(handlePeerFailureSuspicion, epfd);

        // INIT
        stabilizationPeriod = init.getStabilizationPeriod();
        int log2RingSize = init.getLog2RingSize();
        ringSize = new BigInteger("2").pow(log2RingSize);

        int successorListLength = init.getSuccessorListLength();

        self = init.getSelf();

        successorList = new SuccessorList(successorListLength, self,
                ringSize);

        logger = LoggerFactory.getLogger(getClass().getName() + "@"
                + self.getKey().getId());
    }
    private Handler<CreateRing> handleCreateRing = new Handler<CreateRing>() {
        public void handle(CreateRing event) {
            logger.debug("CREATE");

            predecessor = null;
            // successor = localPeer;
            successorList.setSuccessor(self);

            // trigger newSuccessor
            NewSuccessorList newSuccessor = new NewSuccessorList(self,
                    successorList.getSuccessorListView());
            trigger(newSuccessor, ps);

            // trigger newPredecessor
            NewPredecessor newPredecessor = new NewPredecessor(self,
                    predecessor);
            trigger(newPredecessor, ps);

            logger.info("Join Completed");

            // trigger JoinRingCompleted
            JoinRingCompleted joinRingCompleted = new JoinRingCompleted(self);
            trigger(joinRingCompleted, ps);

            // set the stabilization timer
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(
                    stabilizationPeriod, stabilizationPeriod);
            spt.setTimeoutEvent(new StabilizeTimeout(spt));
            trigger(spt, timer);
        }
    };
    private Handler<JoinRing> handleJoinRing = new Handler<JoinRing>() {
        public void handle(JoinRing event) {
            ChordAddress insider = event.getChordInsiders().poll();

            logger.debug("JOIN through " + insider);

            predecessor = null;

            ChordLookupRequest lookupRequest = new ChordLookupRequest(self
                    .getKey(), event.getChordInsiders(), insider);

            trigger(lookupRequest, router);

            // trigger newPredecessor
            NewPredecessor newPredecessor = new NewPredecessor(self,
                    predecessor);
            trigger(newPredecessor, ps);
        }
    };
    @SuppressWarnings("unchecked")
    private Handler<ChordLookupResponse> handleChordLookupResponse = new Handler<ChordLookupResponse>() {
        public void handle(ChordLookupResponse event) {
            if (event.getStatus() == ChordLookupStatus.FAILURE) {
                logger.debug("CHORD_LOOKUP_FAILED");

                LinkedList<ChordAddress> insiders = (LinkedList<ChordAddress>) event
                        .getAttachment();
                ChordAddress nextInsider = insiders.poll();

                // retry join
                ChordLookupRequest request = new ChordLookupRequest(self
                        .getKey(), insiders, nextInsider);

                // cycle through other joining peers
                trigger(request, router);
                return;
            }

            logger.debug("CHORD_LOOKUP_RESP R({})={}", event.getKey(), event
                    .getResponsible());

            // we got the real successor
            successorList.setSuccessor(event.getResponsible());

            neighborAdded(successorList.getSuccessor());

            // trigger GetSuccessorList
            GetSuccessorListRequest request = new GetSuccessorListRequest(
                    RequestState.JOIN, self, successorList.getSuccessor());
            trigger(request, network);

            // trigger newSuccessor
            NewSuccessorList newSuccessor = new NewSuccessorList(self,
                    successorList.getSuccessorListView());
            trigger(newSuccessor, ps);

            logger.info("Join Completed");

            // trigger JoinRingCompleted
            JoinRingCompleted joinRingCompleted = new JoinRingCompleted(self);
            trigger(joinRingCompleted, ps);

            // set the stabilization timer
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(
                    stabilizationPeriod, stabilizationPeriod);
            spt.setTimeoutEvent(new StabilizeTimeout(spt));
            trigger(spt, timer);
        }
    };
    Handler<LeaveRing> handleLeaveRing = new Handler<LeaveRing>() {
        public void handle(LeaveRing event) {
            if (self == null) {
                return;
            }

            if (predecessor != null) {
                trigger(new LeaveNotification(self, predecessor, successorList,
                        predecessor), network);
            }
            if (successorList != null && successorList.getSuccessor() != null) {
                trigger(new LeaveNotification(self, predecessor, successorList,
                        successorList.getSuccessor()), network);
            }
            trigger(new LeaveRingCompleted(self), ps);

            // discard all state
            // self = predecessor = null;
            // successorList = null;
            // TODO discard fdRequests and neighborPeers
        }
    };
    Handler<LeaveNotification> handleLeaveNotification = new Handler<LeaveNotification>() {
        public void handle(LeaveNotification event) {
            ChordAddress leavingPeer = event.getFromPeer();

            if (leavingPeer.equals(predecessor)) {
                // predecessor left
                predecessor = event.getPredecessor();
                // trigger newPredecessor
                NewPredecessor newPredecessor = new NewPredecessor(self,
                        predecessor);
                trigger(newPredecessor, ps);
            }
            if (successorList.getSuccessor().equals(leavingPeer)) {
                // successor left
                successorList.successorFailed(leavingPeer);
                // trigger newSuccessor
                NewSuccessorList newSuccessor = new NewSuccessorList(self,
                        successorList.getSuccessorListView());
                trigger(newSuccessor, ps);
            }
            neighborRemoved(leavingPeer);
        }
    };
    private Handler<GetSuccessorListRequest> handleGetSuccessorListRequest = new Handler<GetSuccessorListRequest>() {
        public void handle(GetSuccessorListRequest event) {
            logger.debug("GET_SUCC_LIST_REQ");

            GetSuccessorListResponse response = new GetSuccessorListResponse(
                    successorList, event.getRequestState(), self, event
                    .getChordSource());
            // reply with successorList
            trigger(response, network);
        }
    };
    private Handler<GetSuccessorListResponse> handleGetSuccessorListResponse = new Handler<GetSuccessorListResponse>() {
        public void handle(GetSuccessorListResponse event) {
            // ignore message if it is not sent by my current successor
            if (!successorList.getSuccessor().equals(event.getChordSource())) {
                return;
            }

            logger.debug("GET_SUCC_LIST_RESP from {} my={} got={}",
                    new Object[]{event.getChordSource(),
                        successorList.getSuccessors(),
                        event.getSuccessorList().getSuccessors()});

            successorList.updateSuccessorList(event.getSuccessorList());

            // trigger newSuccessor
            NewSuccessorList newSuccessorList = new NewSuccessorList(self,
                    successorList.getSuccessorListView());
            trigger(newSuccessorList, ps);
        }
    };
    private Handler<StabilizeTimeout> handleStabilizeTimeout = new Handler<StabilizeTimeout>() {
        public void handle(StabilizeTimeout event) {
            logger.debug("STABILIZATION s={}, p={}", successorList
                    .getSuccessor(), predecessor);

            GetPredecessorRequest request = new GetPredecessorRequest(self,
                    successorList.getSuccessor());
            // send get predecessor request
            trigger(request, network);

            logger.debug("SENT GET_PRED_REQ to {}", successorList
                    .getSuccessor());
        }
    };
    private Handler<GetPredecessorRequest> handleGetPredecessorRequest = new Handler<GetPredecessorRequest>() {
        public void handle(GetPredecessorRequest event) {
            logger.debug("GET_PRED_REQ from {}", event.getChordSource()
                    .getKey());

            GetPredecessorResponse response = new GetPredecessorResponse(
                    predecessor, self, event.getChordSource());
            // reply with predecessor
            trigger(response, network);
        }
    };
    private Handler<GetPredecessorResponse> handleGetPredecessorResponse = new Handler<GetPredecessorResponse>() {
        public void handle(GetPredecessorResponse event) {
            // ignore message if it is not sent by my current successor
            if (!successorList.getSuccessor().equals(event.getChordSource())) {
                return;
            }

            ChordAddress predecessorOfMySuccessor = event.getPredecessor();

            logger.debug("GET_PRED_RESP {} from {} ", predecessorOfMySuccessor,
                    event.getChordSource());

            if (predecessorOfMySuccessor != null) {
                if (predecessorOfMySuccessor.getKey().belongsTo(self.getKey(),
                        successorList.getSuccessor().getKey(),
                        IntervalBounds.OPEN_OPEN, ringSize)) {

                    ChordAddress oldSuccessor = successorList.getSuccessor();
                    ChordAddress newSuccessor = predecessorOfMySuccessor;

                    successorList.setSuccessor(predecessorOfMySuccessor);

                    neighborReplaced(oldSuccessor, newSuccessor);

                    // trigger newSuccessor
                    NewSuccessorList newSuccessorList = new NewSuccessorList(
                            self, successorList.getSuccessorListView());
                    trigger(newSuccessorList, ps);
                }
            }

            // trigger GetSuccessorList
            GetSuccessorListRequest request = new GetSuccessorListRequest(
                    RequestState.STABILIZE, self, successorList.getSuccessor());
            trigger(request, network);

            Notify notify = new Notify(self, successorList.getSuccessor());
            // send notify
            trigger(notify, network);
        }
    };
    private Handler<Notify> handleNotify = new Handler<Notify>() {
        public void handle(Notify event) {
            logger.debug("NOTIFY from {}", event.getChordSource());

            ChordAddress potentialNewPredecessor = event.getFromPeer();

            if (predecessor == null
                    || potentialNewPredecessor.getKey().belongsTo(
                    predecessor.getKey(), self.getKey(),
                    IntervalBounds.OPEN_OPEN, ringSize)) {

                ChordAddress oldPredecessor = predecessor;

                predecessor = potentialNewPredecessor;

                neighborReplaced(oldPredecessor, predecessor);

                // trigger newPredecessor
                NewPredecessor newPredecessor = new NewPredecessor(self,
                        predecessor);
                trigger(newPredecessor, ps);
            }
        }
    };
    private Handler<PeerFailureSuspicion> handlePeerFailureSuspicion = new Handler<PeerFailureSuspicion>() {
        public void handle(PeerFailureSuspicion event) {
            logger.debug("FAILURE_SUSPICION");

            if (event.getSuspicionStatus().equals(SuspicionStatus.SUSPECTED)) {
                // peer is suspected
                ChordAddress suspectedPeer = neighborPeers.get(event
                        .getPeerAddress());

                if (suspectedPeer == null
                        || !fdRequests.containsKey(suspectedPeer
                        .getPeerAddress())) {
                    // due to component concurrency it is possible that the FD
                    // component sent us a suspicion event right after we sent
                    // it a stop monitor request
                    return;
                }

                if (suspectedPeer.equals(predecessor)) {
                    // predecessor suspected
                    predecessor = null;

                    // trigger newPredecessor
                    NewPredecessor newPredecessor = new NewPredecessor(self,
                            predecessor);
                    trigger(newPredecessor, ps);
                    neighborRemoved(suspectedPeer);
                }
                if (successorList.getSuccessors().contains(suspectedPeer)) {
                    // successor suspected
                    ChordAddress oldSuccessor = successorList.getSuccessor();

                    successorList.successorFailed(suspectedPeer);

                    ChordAddress newSuccessor = successorList.getSuccessor();

                    neighborReplaced(oldSuccessor, newSuccessor);

                    // trigger newSuccessor
                    NewSuccessorList newSuccessorList = new NewSuccessorList(
                            self, successorList.getSuccessorListView());
                    trigger(newSuccessorList, ps);
                }
            } else {
                // peer is alive again
            }
        }
    };

    private final void neighborReplaced(ChordAddress oldPeer,
            ChordAddress newPeer) {
        if (oldPeer == null) {
            if (newPeer == null) {
                // both null
                return;
            } else {
                // old is null
                neighborAdded(newPeer);
            }
        } else {
            if (newPeer == null) {
                // new is null
                neighborRemoved(oldPeer);
            } else {
                // none is null
                if (!newPeer.equals(oldPeer)) {
                    // different peers
                    neighborAdded(newPeer);
                    neighborRemoved(oldPeer);
                }
            }
        }
    }

    private final void neighborAdded(ChordAddress peer) {
        if (!peer.equals(self)) {
            Address addr = peer.getPeerAddress();
            if (!neighborPeers.containsKey(addr)) {
                // start failure detection on new neighbor
                StartProbingPeer spp = new StartProbingPeer(addr, peer);
                neighborPeers.put(addr, peer);
                neighborCounters.put(addr, 1);
                fdRequests.put(addr, spp.getRequestId());
                trigger(spp, epfd);
            } else {
                // already a neighbor
                neighborCounters.put(addr, 1 + neighborCounters.get(addr));
            }
            //
            // System.err.println(neighborCounters.get(addr) +
            // " NEIGHBOR_ADDED "
            // + peer + " AT " + self + " MY=" + neighborPeers.values()
            // + " C=" + neighborCounters + " called from "
            // + Thread.currentThread().getStackTrace()[3] + " and from "
            // + Thread.currentThread().getStackTrace()[4]);
        }
    }

    private final void neighborRemoved(ChordAddress peer) {
        if (!peer.equals(self)) {
            Address addr = peer.getPeerAddress();
            if (neighborPeers.containsKey(addr)) {
                int count = neighborCounters.get(addr);
                count--;
                if (count == 0) {
                    // stop failure detection on neighbor
                    neighborPeers.remove(addr);
                    neighborCounters.remove(addr);
                    trigger(new StopProbingPeer(addr, fdRequests.get(addr)),
                            epfd);
                    fdRequests.remove(addr);
                } else {
                    // still a neighbor
                    neighborCounters.put(addr, count);
                }
            }
            //
            // System.err.println(neighborCounters.get(addr)
            // + " NEIGHBOR_REMOVED " + peer + " AT " + self + " MY="
            // + neighborPeers.values() + " C=" + neighborCounters
            // + " called from "
            // + Thread.currentThread().getStackTrace()[3] + " and from "
            // + Thread.currentThread().getStackTrace()[4]);
        }
    }
}
