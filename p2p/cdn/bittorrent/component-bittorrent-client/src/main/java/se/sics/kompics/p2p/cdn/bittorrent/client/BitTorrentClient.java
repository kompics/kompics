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
package se.sics.kompics.p2p.cdn.bittorrent.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.cdn.bittorrent.BitTorrentConfiguration;
import se.sics.kompics.p2p.cdn.bittorrent.TorrentMetadata;
import se.sics.kompics.p2p.cdn.bittorrent.address.BitTorrentAddress;
import se.sics.kompics.p2p.cdn.bittorrent.client.messages.BitfieldMessage;
import se.sics.kompics.p2p.cdn.bittorrent.client.messages.CancelMessage;
import se.sics.kompics.p2p.cdn.bittorrent.client.messages.ChokeMessage;
import se.sics.kompics.p2p.cdn.bittorrent.client.messages.HandshakeMessage;
import se.sics.kompics.p2p.cdn.bittorrent.client.messages.HaveMessage;
import se.sics.kompics.p2p.cdn.bittorrent.client.messages.InterestedMessage;
import se.sics.kompics.p2p.cdn.bittorrent.client.messages.KeepAliveMessage;
import se.sics.kompics.p2p.cdn.bittorrent.client.messages.NotInterestedMessage;
import se.sics.kompics.p2p.cdn.bittorrent.client.messages.PieceMessage;
import se.sics.kompics.p2p.cdn.bittorrent.client.messages.RequestMessage;
import se.sics.kompics.p2p.cdn.bittorrent.client.messages.UnchokeMessage;
import se.sics.kompics.p2p.cdn.bittorrent.tracker.TrackerRequestMessage;
import se.sics.kompics.p2p.cdn.bittorrent.tracker.TrackerResponseMessage;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;

/**
 * The
 * <code>BitTorrentPeer</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class BitTorrentClient extends ComponentDefinition {

    Negative<BitTorrentClientPort> bt = negative(BitTorrentClientPort.class);
    Positive<Network> network = positive(Network.class);
    Positive<Timer> timer = positive(Timer.class);
    private Logger logger;
    private TorrentMetadata torrent;
    private BitTorrentConfiguration configuration;
    // peer state
    private BitTorrentAddress self;
    private boolean iAmSeeding, joining, randomFirst, rarestFirst;
    private Bitfield myPieces, piecesInTransit;
    private int chokingRound;
    private PieceAvailability pieceAvailability;
    private HashMap<BitTorrentAddress, PeerInfo> peers;
    private ArrayList<BitTorrentAddress> chokedPeers;
    private ArrayList<BitTorrentAddress> interestedPeers;
    private ArrayList<BitTorrentAddress> chokedInterestedPeers;
    private ArrayList<BitTorrentAddress> unchokedPeers;
    private ArrayList<BitTorrentAddress> activePeers;
    private HashSet<BitTorrentAddress> uploaders;
    private HashMap<Integer, PieceInTransit> partialPieces;
    private HashSet<BitTorrentAddress> unchokedNotInterestedPeers;
    private BitTorrentAddress plannedOptimisticUnchokedPeer;
    private Random random;

    public BitTorrentClient(BitTorrentClientInit init) {
        peers = new HashMap<BitTorrentAddress, PeerInfo>();
        chokedPeers = new ArrayList<BitTorrentAddress>();
        interestedPeers = new ArrayList<BitTorrentAddress>();
        chokedInterestedPeers = new ArrayList<BitTorrentAddress>();
        unchokedPeers = new ArrayList<BitTorrentAddress>();
        activePeers = new ArrayList<BitTorrentAddress>();
        unchokedNotInterestedPeers = new HashSet<BitTorrentAddress>();

        uploaders = new HashSet<BitTorrentAddress>();
        partialPieces = new HashMap<Integer, PieceInTransit>();

        subscribe(handleStart, control);

        subscribe(handleJoin, bt);
        subscribe(handleReChoke, timer);
        subscribe(handleSendKeepAlive, timer);
        subscribe(handleTrackerResponse, network);
        subscribe(handleHandshake, network);
        subscribe(handleBitfield, network);
        subscribe(handleHave, network);
        subscribe(handleInterested, network);
        subscribe(handleNotInterested, network);
        subscribe(handleChoke, network);
        subscribe(handleUnchoke, network);
        subscribe(handleKeepAlive, network);
        subscribe(handleRequest, network);
        subscribe(handlePiece, network);
        subscribe(handleCancel, network);

        // INIT

        self = init.getSelf();
        random = new Random(self.getPeerId().longValue());

        torrent = init.getTorrent();
        configuration = init.getConfiguration();

        iAmSeeding = false;
        myPieces = init.getInitialPieces();
        piecesInTransit = new Bitfield(torrent.getPieceCount());

        pieceAvailability = new PieceAvailability(myPieces.getSize());
        pieceAvailability.addPeer(myPieces);

        chokingRound = -1;
    }
    Handler<Start> handleStart = new Handler<Start>() {
        @Override
        public void handle(Start init) {


            if (myPieces.allSet()) {
                iAmSeeding = true;
                trigger(new DownloadCompleted(self), bt);
            } else if (myPieces.cardinality() > configuration
                    .getRandomPieceCount()) {
                rarestFirst = true;
                randomFirst = false;
            } else {
                rarestFirst = false;
                randomFirst = true;
            }



            logger = LoggerFactory.getLogger("se.sics.kompics.p2p.cdn.bittorrent"
                    + ".client.BitTorrentClient@" + self.getPeerId());
            logger.info("Initialized. I have {} pieces out of {}", myPieces
                    .cardinality(), myPieces.getSize());
        }
    };
    Handler<JoinSwarm> handleJoin = new Handler<JoinSwarm>() {
        public void handle(JoinSwarm event) {
            logger.info("Joining.");

            joining = true;
            // contact tracker
            TrackerRequestMessage request = new TrackerRequestMessage(self,
                    torrent.getTracker(), torrent.getTorrentId(), "0", "0",
                    "left", configuration.getMaxInitiatedConnections(), null);
            trigger(request, network);
        }
    };
    Handler<TrackerResponseMessage> handleTrackerResponse = new Handler<TrackerResponseMessage>() {
        public void handle(TrackerResponseMessage event) {
            if (joining) {
                // schedule choke algorithm
                SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(
                        configuration.getChokingPeriod(), configuration
                        .getChokingPeriod());
                spt.setTimeoutEvent(new ReChoke(spt));
                trigger(spt, timer);

                // schedule sending keep-alives
                spt = new SchedulePeriodicTimeout(configuration
                        .getKeepAlivePeriod(), configuration
                        .getKeepAlivePeriod());
                spt.setTimeoutEvent(new SendKeepAlive(spt));
                trigger(spt, timer);

                joining = false;
            }

            // connect to peers returned by tracker
            LinkedList<BitTorrentAddress> peers = event.getPeers();

            for (BitTorrentAddress peer : peers) {
                HandshakeMessage handshake = new HandshakeMessage(self, peer,
                        torrent.getTorrentId(), false);
                trigger(handshake, network);

                BitfieldMessage bfm = new BitfieldMessage(self, peer, myPieces);
                trigger(bfm, network);

                logger.debug("Initiated HANDSHAKE and BITFIELD to {}", peer);
            }
        }
    };
    Handler<HandshakeMessage> handleHandshake = new Handler<HandshakeMessage>() {
        public void handle(HandshakeMessage event) {
            if (peers.size() >= configuration.getMaxPeers()) {
                return;
            }
            BitTorrentAddress peer = event.getBitTorrentSource();
            logger.debug("Got HANDSHAKE from {}", peer);

            if (event.isResponse()) {
                // received a response to a handshake initiated by us
                PeerInfo peerInfo = new PeerInfo(peer, true, configuration,
                        torrent);
                peers.put(peer, peerInfo);
                chokedPeers.add(peer);
            } else {
                // peer initiated handshake. We accept and respond.
                PeerInfo peerInfo = new PeerInfo(peer, false, configuration,
                        torrent);
                peers.put(peer, peerInfo);
                chokedPeers.add(peer);

                HandshakeMessage handshake = new HandshakeMessage(self, peer,
                        torrent.getTorrentId(), true);
                trigger(handshake, network);

                BitfieldMessage bfm = new BitfieldMessage(self, peer, myPieces);
                trigger(bfm, network);

                logger.debug("Responded with HANDSHAKE and BITFIELD to {}",
                        peer);
            }
        }
    };
    Handler<BitfieldMessage> handleBitfield = new Handler<BitfieldMessage>() {
        public void handle(BitfieldMessage event) {
            BitTorrentAddress peer = event.getBitTorrentSource();

            logger.debug("Got BITFIELD from {}", peer);

            PeerInfo peerInfo = peers.get(peer);

            if (peerInfo != null) {
                Bitfield bitfield = event.getBitfield();
                peerInfo.pieces.setAll(bitfield);
                pieceAvailability.addPeer(bitfield);

                // check whether we are interested
                if (peerInfo.becameInteresting(myPieces)) {
                    trigger(new InterestedMessage(self, peer), network);
                }
            }
        }
    };
    Handler<HaveMessage> handleHave = new Handler<HaveMessage>() {
        public void handle(HaveMessage event) {
            BitTorrentAddress peer = event.getBitTorrentSource();
            int piece = event.getPieceIndex();

            logger.debug("Got HAVE {} from {}", piece, peer);

            PeerInfo peerInfo = peers.get(peer);
            if (peerInfo != null) {
                peerInfo.pieces.set(piece);
                pieceAvailability.addPiece(piece);

                // check whether we are interested
                if (peerInfo.becameInteresting(myPieces)) {
                    trigger(new InterestedMessage(self, peer), network);
                }
            }
        }
    };
    Handler<InterestedMessage> handleInterested = new Handler<InterestedMessage>() {
        public void handle(InterestedMessage event) {
            BitTorrentAddress peer = event.getBitTorrentSource();

            logger.debug("Got INTERESTED from {}", peer);

            PeerInfo peerInfo = peers.get(peer);
            if (peerInfo != null) {
                peerInfo.peerInterested = true;
                if (!interestedPeers.contains(peer)) {
                    interestedPeers.add(peer);
                }
                if (peerInfo.amChoking && !chokedInterestedPeers.contains(peer)) {
                    chokedInterestedPeers.add(peer);
                }
                if (unchokedNotInterestedPeers.contains(peer)) {
                    // run choking algorithm when unchoked peer becomes
                    // interested
                    chokeUnchoke(false);
                }
            }
        }
    };
    Handler<NotInterestedMessage> handleNotInterested = new Handler<NotInterestedMessage>() {
        public void handle(NotInterestedMessage event) {
            BitTorrentAddress peer = event.getBitTorrentSource();

            logger.debug("Got NOT_INTERESTED from {}", peer);

            PeerInfo peerInfo = peers.get(peer);
            if (peerInfo != null) {
                boolean wasInterested = peerInfo.peerInterested;
                peerInfo.peerInterested = false;
                interestedPeers.remove(peer);
                chokedInterestedPeers.remove(peer);

                if (wasInterested && unchokedPeers.contains(peer)) {
                    // run choking algorithm when unchoked peer becomes not
                    // interested
                    chokeUnchoke(false);
                }
            }
        }
    };
    Handler<ChokeMessage> handleChoke = new Handler<ChokeMessage>() {
        public void handle(ChokeMessage event) {
            BitTorrentAddress peer = event.getBitTorrentSource();

            logger.debug("Got CHOKED from {}", peer);

            PeerInfo peerInfo = peers.get(peer);
            if (peerInfo != null) {
                peerInfo.peerChoking = true;

                if (uploaders.contains(peer)) {
                    uploaders.remove(peer);
                }
            }
        }
    };
    Handler<UnchokeMessage> handleUnchoke = new Handler<UnchokeMessage>() {
        public void handle(UnchokeMessage event) {
            BitTorrentAddress peer = event.getBitTorrentSource();

            logger.debug("Got UNCHOKED from {}", peer);

            PeerInfo peerInfo = peers.get(peer);
            if (peerInfo != null) {
                peerInfo.peerChoking = false;
                // peer unchoked me. I can now download from it.
                if (!uploaders.contains(peer)) {
                    uploaders.add(peer);
                    startDownloadingPieceFrom(peer, configuration
                            .getRequestPipelineLength());
                }
            }
        }
    };
    Handler<RequestMessage> handleRequest = new Handler<RequestMessage>() {
        public void handle(RequestMessage event) {
            BitTorrentAddress peer = event.getBitTorrentSource();
            int piece = event.getPieceIndex();

            // logger.debug("Got REQUEST({}, {}) from {}", new Object[] { piece,
            // event.getBegin() / configuration.getBlockSize(), peer });

            if (!activePeers.contains(peer) || !myPieces.has(piece)) {
                // only upload to the unchoked interested peers
                return;
            }

            PieceMessage pieceMessage = new PieceMessage(self, peer, piece,
                    event.getBegin(), configuration.getBlockSize(), new byte[0]);
            trigger(pieceMessage, network);

            // mark the we uploaded a block
            PeerInfo peerInfo = peers.get(peer);
            peerInfo.uploadHistory.transferredBlock();
        }
    };
    Handler<PieceMessage> handlePiece = new Handler<PieceMessage>() {
        public void handle(PieceMessage event) {
            // received piece block
            int piece = event.getPieceIndex();
            if (!piecesInTransit.get(piece)) {
                System.err.println(piece);
                return;
            }

            BitTorrentAddress peer = event.getBitTorrentSource();

            // logger.debug("Got PIECE({}, {}) from {}", new Object[] { piece,
            // event.getBegin() / configuration.getBlockSize(), peer });

            // mark the we downloaded a block
            PeerInfo peerInfo = peers.get(peer);

            if (peerInfo == null) {
                return;
            }

            peerInfo.downloadHistory.transferredBlock();

            PieceInTransit transit = partialPieces.get(piece);

            int blockIndex = event.getBegin() / configuration.getBlockSize();
            boolean completedPiece = transit.blockReceived(blockIndex);

            peerInfo.requestPipeline.remove(new Block(piece, blockIndex));

            if (completedPiece) {
                pieceCompleted(piece, peer);
                partialPieces.remove(piece);
            }

            // if peer not choking us
            if (!peerInfo.peerChoking && uploaders.contains(peer)) {
                // try to continue requesting further blocks of this piece
                int newBlockIndex = transit.getNextBlockToRequest();

                if (newBlockIndex == -1) {
                    // requested all blocks of this piece. We can now try to
                    // select a new piece to request from this peer.
                    Block lastRequestedBlock = peerInfo.requestPipeline
                            .peekLast();

                    if (lastRequestedBlock == null
                            || lastRequestedBlock.getPieceIndex() == piece) {
                        // start downloading a new piece
                        startDownloadingPieceFrom(peer, 1);
                    } else {
                        // continue downloading a block from the last requested
                        // piece
                        int lastRequestedPiece = lastRequestedBlock
                                .getPieceIndex();
                        PieceInTransit latestTransit = partialPieces
                                .get(lastRequestedPiece);

                        int blockToRequest = -1;
                        if (latestTransit != null) {
                            blockToRequest = latestTransit.getNextBlockToRequest();
                        }
                        if (blockToRequest != -1) {
                            // request the next block of the last requested
                            // piece
                            RequestMessage request = new RequestMessage(self,
                                    peer, lastRequestedPiece, blockToRequest
                                    * configuration.getBlockSize(),
                                    configuration.getBlockSize());
                            trigger(request, network);

                            peerInfo.requestPipeline.add(new Block(
                                    lastRequestedPiece, blockToRequest));
                        } else {
                            // all blocks were requested from the last requested
                            // piece. we request a new piece
                            startDownloadingPieceFrom(peer, 1);
                        }
                    }
                } else {
                    // we just request the next block of this piece
                    RequestMessage request = new RequestMessage(self, peer,
                            piece,
                            newBlockIndex * configuration.getBlockSize(),
                            configuration.getBlockSize());
                    trigger(request, network);

                    peerInfo.requestPipeline
                            .add(new Block(piece, newBlockIndex));
                }
            }
        }
    };
    Handler<SendKeepAlive> handleSendKeepAlive = new Handler<SendKeepAlive>() {
        public void handle(SendKeepAlive event) {
            long keepAliveTimeout = 2 * configuration.getKeepAlivePeriod();
            long latest = System.currentTimeMillis() - keepAliveTimeout;

            // every 2 minutes send keep alives to all neighbors
            // and check that a keep-alive was received in the last 4 mins
            LinkedList<PeerInfo> neighbors = new LinkedList<PeerInfo>(peers
                    .values());
            for (PeerInfo peerInfo : neighbors) {
                if (peerInfo.lastKeepAliveReceivedAt < latest) {
                    // remove silent neighbor
                    removeNeighbor(peerInfo.peer);
                } else {
                    KeepAliveMessage keepAlive = new KeepAliveMessage(self,
                            peerInfo.peer);
                    trigger(keepAlive, network);
                }
            }
        }
    };
    Handler<KeepAliveMessage> handleKeepAlive = new Handler<KeepAliveMessage>() {
        public void handle(KeepAliveMessage event) {
            PeerInfo info = peers.get(event.getBitTorrentSource());
            if (info != null) {
                info.lastKeepAliveReceivedAt = System.currentTimeMillis();
            }
        }
    };
    Handler<CancelMessage> handleCancel = new Handler<CancelMessage>() {
        public void handle(CancelMessage event) {
            // END-GAME NOT IMPLEMENTED
        }
    };
    Handler<ReChoke> handleReChoke = new Handler<ReChoke>() {
        public void handle(ReChoke event) {
            chokeUnchoke(true);
        }
    };

    // ============= DATA TRANSFER =============
    private void pieceCompleted(int piece, BitTorrentAddress peer) {
        // PeerInfo peerInfo = peers.get(peer);
        // downloaded a complete piece
        piecesInTransit.reset(piece);
        myPieces.set(piece);

        if (myPieces.cardinality() == configuration.getRandomPieceCount()) {
            // we switch piece selection strategy from random- to rarest-first
            randomFirst = false;
            rarestFirst = true;
        }

        // send a HAVE message to all neighbors
        for (BitTorrentAddress neighbor : peers.keySet()) {
            HaveMessage haveMessage = new HaveMessage(self, neighbor, piece);
            trigger(haveMessage, network);

            // check whether we are still interested in this neighbor
            PeerInfo neighborInfo = peers.get(neighbor);
            if (neighborInfo != null
                    && neighborInfo.becameNotInteresting(myPieces)) {
                trigger(new NotInterestedMessage(self, neighbor), network);
            }
        }

        if (myPieces.allSet()) {
            // we got the last piece. we become a seed
            iAmSeeding = true;
            trigger(new DownloadCompleted(self), bt);

            // run choking algorithm
            chokeUnchoke(false);
        }
    }

    private void startDownloadingPieceFrom(BitTorrentAddress peer,
            int requestBlocks) {
        int piece = selectPiece(peer);
        if (piece == -1) {
            // no piece is eligible for download from this peer
            return;
        }
        piecesInTransit.set(piece);
        int blockCount = torrent.getPieceSize() / configuration.getBlockSize();
        PieceInTransit transit = new PieceInTransit(piece, blockCount, peer);
        partialPieces.put(piece, transit);

        PeerInfo peerInfo = peers.get(peer);

        for (int i = 0; i < requestBlocks; i++) {
            int nextBlock = transit.getNextBlockToRequest();
            int offset = nextBlock * configuration.getBlockSize();

            RequestMessage request = new RequestMessage(self, peer, piece,
                    offset, configuration.getBlockSize());

            trigger(request, network);
            peerInfo.requestPipeline.add(new Block(piece, nextBlock));
        }
    }

    // ============= PIECE SELECTION =============
    /**
     * selects a new piece to request, according to the piece selection mode.
     * The selected piece is not available locally, it is not in transit, and it
     * is available at the
     * <code>peer</code> node.
     *
     * @param peer
     * @return the new piece to request or -1 if no piece is eligible.
     */
    private int selectPiece(BitTorrentAddress peer) {
        PeerInfo info = peers.get(peer);
        if (info == null) {
            return -1;
        }

        // first we try to complete a stale piece before selecting a new piece
        // [strict piece selection policy]
        int stalePiece = selectStalePiece(peer);
        if (stalePiece != -1) {
            return stalePiece;
        }

        // compute the set of eligible pieces
        Bitfield eligible = info.pieces.copy();
        eligible.andNot(myPieces);
        eligible.andNot(piecesInTransit);

        if (eligible.isEmpty()) {
            return -1;
        }
        // select a piece from the eligible pieces
        int piece = -1;
        if (randomFirst) {
            // select a random eligible piece
            int lastSet = eligible.length();
            piece = random.nextInt(lastSet);
            piece = eligible.nextSetBit(piece);
            return piece;
        } else if (rarestFirst) {
            // select an eligible piece which is rarest in our neighborhood
            ArrayList<Integer> rarestPiecesFirst = new ArrayList<Integer>();
            for (int i = eligible.nextSetBit(0); i >= 0; i = eligible
                            .nextSetBit(i + 1)) {
                rarestPiecesFirst.add(i);
            }
            // sort eligible pieces by their availability in our neighborhood
            Collections
                    .sort(rarestPiecesFirst, pieceAvailability.lowestFirst());
            int minAvailability = pieceAvailability
                    .getAvailability(rarestPiecesFirst.get(0));
            // the index of the first piece that is more available than minAvail
            int nextMinIndex = rarestPiecesFirst.size();
            for (int i = 0; i < rarestPiecesFirst.size(); i++) {
                int pieceAvail = pieceAvailability
                        .getAvailability(rarestPiecesFirst.get(i));
                if (pieceAvail > minAvailability) {
                    nextMinIndex = i;
                    break;
                }
            }
            // pick a random piece out of the least available ones
            int randomPiece = random.nextInt(nextMinIndex);
            piece = rarestPiecesFirst.get(randomPiece);
            return piece;
        } else {
            return piece;
        }
    }

    /**
     * implements the strict piece selection policy. A started piece which
     * became stale is tried to be found and further blocks requested
     *
     * @return
     */
    private int selectStalePiece(BitTorrentAddress peer) {
        PeerInfo info = peers.get(peer);
        if (info == null) {
            return -1;
        }
        Bitfield eligible = info.pieces.copy();
        eligible.and(piecesInTransit);
        // eligible contains the pieces in transit that the peer has
        // we look for stale ones

        for (int piece = eligible.nextSetBit(0); piece >= 0; piece = eligible
                        .nextSetBit(piece + 1)) {
            PieceInTransit transit = partialPieces.get(piece);
            if (transit.isStalePiece(60000)) {
                // discard old stale piece and select it again
                partialPieces.remove(piece);
                piecesInTransit.reset(piece);
                for (PeerInfo inf : peers.values()) {
                    inf.discardPiece(piece);
                }
                return piece;
            }
        }
        return -1;
    }

    private void tryToRestartTransfersFromDiscardedUploaders() {
        for (BitTorrentAddress peer : uploaders) {
            PeerInfo info = peers.get(peer);
            if (info.requestPipeline.isEmpty()) {
                startDownloadingPieceFrom(peer, configuration
                        .getRequestPipelineLength());
            }
        }
    }

    // ============= PEER SELECTION (CHOKING ALGORITHM) =============
    /**
     * implements the (re)choking algorithm. Called both periodically (10s), and
     * also when an unchoked peer becomes interested or not interested, and when
     * a peer leaves the neighborhood set
     *
     * @param regularRound is <code>true</code> when algorithm is called
     * periodically and <code>false</code> when called as a result of change of
     * peer interest
     */
    private void chokeUnchoke(boolean regularRound) {
        tryToRestartTransfersFromDiscardedUploaders();
        logger.debug("RECHOKING");
        logDebugState();
        if (regularRound) {
            chokingRound++;
        }
        if (iAmSeeding) {
            seedChoke();
        } else {
            leecherChoke();
        }
        logDebugState();
        logger.debug("RECHOKED");
    }

    /**
     * choking algorithm in seeding mode
     */
    private void seedChoke() {
        // step 1
        LinkedList<PeerInfo> interestedRecentlyUnchokedPeers = selectInterestedRecentlyUnchokedPeers();
        sortByLastUnchokeTime(interestedRecentlyUnchokedPeers);

        // step 2
        sortByCurrentDownloadingRate(tmpAllOtherPeers);

        LinkedList<BitTorrentAddress> sortedPeers = new LinkedList<BitTorrentAddress>();
        for (PeerInfo peerInfo : interestedRecentlyUnchokedPeers) {
            sortedPeers.add(peerInfo.peer);
        }
        for (PeerInfo peerInfo : tmpAllOtherPeers) {
            sortedPeers.add(peerInfo.peer);
        }

        // step 3
        HashSet<BitTorrentAddress> toUnchoke = new HashSet<BitTorrentAddress>();
        if (chokingRound % 3 == 0) {
            // every third round unchoke the first 4 peers
            int p = configuration.getActivePeersCount();
            for (BitTorrentAddress peer : sortedPeers) {
                toUnchoke.add(peer);
                if (--p == 0) {
                    break;
                }
            }
        } else {
            // 2 out of 3 rounds unchoke the first 3 peers + 1 random interested
            int p = configuration.getActivePeersCount() - 1;
            for (BitTorrentAddress peer : sortedPeers) {
                toUnchoke.add(peer);
                if (--p == 0) {
                    break;
                }
            }
            BitTorrentAddress peer = selectRandomInterestedPeer(toUnchoke);
            if (peer != null) {
                toUnchoke.add(peer);
            }
        }
        doUnchoke(toUnchoke);
    }

    /**
     * choking algorithm in leecher mode
     */
    private void leecherChoke() {
        unchokedNotInterestedPeers.clear();

        // step 1
        if (chokingRound % 3 == 0) {
            // every third round select a random choked and interested POU peer
            plannedOptimisticUnchokedPeer = selectPlannedOptimisticUnchokedPeer();
        }
        // step 2
        LinkedList<PeerInfo> interestedUploadingPeers = selectInterestedUploadingPeers();
        sortByCurrentUploadingRate(interestedUploadingPeers);

        // step 3
        HashSet<BitTorrentAddress> regularUnchokedPeers = new HashSet<BitTorrentAddress>();
        int p = configuration.getActivePeersCount() - 1;
        for (PeerInfo peerInfo : interestedUploadingPeers) {
            regularUnchokedPeers.add(peerInfo.peer);
            if (--p == 0) {
                break;
            }
        }
        // step 4
        if (!regularUnchokedPeers.contains(plannedOptimisticUnchokedPeer)) {
            doUnchoke(regularUnchokedPeers);
            return;
        }
        // step5
        do {
            plannedOptimisticUnchokedPeer = selectRandomPeer(
                    regularUnchokedPeers, unchokedNotInterestedPeers);
            if (plannedOptimisticUnchokedPeer == null) {
                // all peers are now unchoked
                doUnchoke(regularUnchokedPeers);
                return;
            }

            PeerInfo info = peers.get(plannedOptimisticUnchokedPeer);
            if (info.peerInterested) {
                doUnchoke(regularUnchokedPeers);
                return;
            } else {
                unchokedNotInterestedPeers.add(plannedOptimisticUnchokedPeer);
            }
        } while (true);
    }

    /**
     * actually unchokes the peers in: (1) regularUnchokedPeers, (2)
     * plannedOptimisticUnchokedPeer, and (3) unchokedNotInterestedPeers. chokes
     * all other peers.
     *
     * When in seeding mode it only unchokes the peers in
     * <code>regularUnchokedPeers</code>
     */
    private void doUnchoke(HashSet<BitTorrentAddress> regularUnchokedPeers) {
        HashSet<BitTorrentAddress> toUnchoke = new HashSet<BitTorrentAddress>(
                regularUnchokedPeers);
        if (!iAmSeeding) {
            toUnchoke.addAll(unchokedNotInterestedPeers);
            if (plannedOptimisticUnchokedPeer != null) {
                toUnchoke.add(plannedOptimisticUnchokedPeer);
            }
        }

        // the toUnchoke set contains the peers that should be unchoked in the
        // next round. We have 4 types of peers:
        // A: in toUnchoke and already unchoked: do nothing
        // B: in toUnchoke and currently choked: unchoke them
        // C: not in toUnchoke and currently unchoked: choke them
        // D: not in toUnchoke and currently choked: do nothing

        HashSet<BitTorrentAddress> peersToChoke = new HashSet<BitTorrentAddress>(
                unchokedPeers);
        peersToChoke.removeAll(toUnchoke);
        HashSet<BitTorrentAddress> peersToUnchoke = new HashSet<BitTorrentAddress>(
                chokedPeers);
        peersToUnchoke.retainAll(toUnchoke);

        // reset activePeers
        activePeers = new ArrayList<BitTorrentAddress>();
        activePeers.addAll(regularUnchokedPeers);
        if (!iAmSeeding && plannedOptimisticUnchokedPeer != null) {
            activePeers.add(plannedOptimisticUnchokedPeer);
        }

        for (BitTorrentAddress peer : peersToUnchoke) {
            unchokePeer(peer);
        }
        for (BitTorrentAddress peer : peersToChoke) {
            chokePeer(peer);
        }
    }

    private void unchokePeer(BitTorrentAddress peer) {
        chokedPeers.remove(peer);
        chokedInterestedPeers.remove(peer);
        PeerInfo peerInfo = peers.get(peer);
        if (peerInfo == null) {
            return;
        }
        unchokedPeers.add(peer);

        peerInfo.amChoking = false;
        peerInfo.lastUnchoked = System.currentTimeMillis();
        trigger(new UnchokeMessage(self, peer), network);
    }

    private void chokePeer(BitTorrentAddress peer) {
        PeerInfo peerInfo = peers.get(peer);
        unchokedPeers.remove(peer);

        if (peerInfo == null) {
            return;
        }
        chokedPeers.add(peer);
        if (peerInfo.peerInterested) {
            chokedInterestedPeers.add(peer);
        }

        peerInfo.amChoking = true;
        trigger(new ChokeMessage(self, peer), network);
    }

    /**
     * sorts given peer list descendingly by their current upload rate
     *
     * @param interestedUploadingPeers
     */
    private void sortByCurrentUploadingRate(
            LinkedList<PeerInfo> interestedUploadingPeers) {

        for (PeerInfo peerInfo : interestedUploadingPeers) {
            peerInfo.computeCurrentUploadRate(configuration
                    .getTransferRateWindow());
        }

        Collections.sort(interestedUploadingPeers, PeerInfo.sortByUploadRate);
    }

    /**
     * sorts given peer list descendingly by their last unchoke time
     *
     * @param interestedRecentlyUnchokedPeers
     */
    private void sortByLastUnchokeTime(
            LinkedList<PeerInfo> interestedRecentlyUnchokedPeers) {
        for (PeerInfo peerInfo : interestedRecentlyUnchokedPeers) {
            peerInfo.computeCurrentDownloadRate(configuration
                    .getTransferRateWindow());
        }
        Collections.sort(interestedRecentlyUnchokedPeers,
                PeerInfo.sortByLastUnchoke);
    }

    /**
     * sorts given peer list descendingly by their current download rate from us
     *
     * @param allOtherPeers
     */
    private void sortByCurrentDownloadingRate(LinkedList<PeerInfo> allOtherPeers) {
        for (PeerInfo peerInfo : allOtherPeers) {
            peerInfo.computeCurrentDownloadRate(configuration
                    .getTransferRateWindow());
        }
        Collections.sort(allOtherPeers, PeerInfo.sortByDownloadRate);
    }

    /**
     * @return a list of peers that are interested and have uploaded at least
     * one block in the last 30 seconds
     */
    private LinkedList<PeerInfo> selectInterestedUploadingPeers() {
        LinkedList<PeerInfo> list = new LinkedList<PeerInfo>();

        for (BitTorrentAddress peer : interestedPeers) {
            PeerInfo info = peers.get(peer);
            if (info.downloadHistory.getBlocksTranferredInTheLast(configuration
                    .getSnubbedWindow()) > 0) {
                // this test filters out peers who did not upload any block to
                // us in the last 30s. Skip it by changing ">" to ">="
                list.add(info);
            }
        }
        return list;
    }
    LinkedList<PeerInfo> tmpAllOtherPeers;

    /**
     * @return a list of peers that are interested and were unchoked in the last
     * 20 seconds or have pending block requests
     */
    private LinkedList<PeerInfo> selectInterestedRecentlyUnchokedPeers() {
        LinkedList<PeerInfo> list = new LinkedList<PeerInfo>();
        tmpAllOtherPeers = new LinkedList<PeerInfo>();

        for (BitTorrentAddress peer : interestedPeers) {
            PeerInfo info = peers.get(peer);

            if (info.isLastUnchokeInTheLast(configuration.getUnchokeWindow())) {
                list.add(info);
            } else {
                tmpAllOtherPeers.add(info);
            }
        }
        return list;
    }

    /**
     * @return a random peer that is both choked and interested
     */
    private BitTorrentAddress selectPlannedOptimisticUnchokedPeer() {
        if (chokedInterestedPeers.isEmpty()) {
            // when we have no interested and choked peers we do no optimistic
            // unchoke
            return null;
        }

        int randomPeer = random.nextInt(chokedInterestedPeers.size());
        return chokedInterestedPeers.get(randomPeer);
    }

    /**
     * @param except
     * @return a random peer that is not yet unchoked
     */
    private BitTorrentAddress selectRandomPeer(
            HashSet<BitTorrentAddress> except1,
            HashSet<BitTorrentAddress> except2) {

        LinkedList<BitTorrentAddress> eligible = new LinkedList<BitTorrentAddress>(
                peers.keySet());
        eligible.removeAll(except1);
        eligible.removeAll(except2);

        if (eligible.isEmpty()) {
            // all unchoked peers have been selected
            return null;
        }
        int randomPeer = random.nextInt(eligible.size());
        return eligible.get(randomPeer);
    }

    /**
     * @param except
     * @return a random interested peer that is not in <code>except</code>
     */
    private BitTorrentAddress selectRandomInterestedPeer(
            HashSet<BitTorrentAddress> except) {

        LinkedList<BitTorrentAddress> eligible = new LinkedList<BitTorrentAddress>(
                interestedPeers);
        eligible.removeAll(except);

        if (eligible.isEmpty()) {
            // all interested peers have been selected
            return null;
        }
        int randomPeer = random.nextInt(eligible.size());
        return eligible.get(randomPeer);
    }

    private void removeNeighbor(BitTorrentAddress peer) {
        // remove all references to this neighbor
        chokedPeers.remove(peer);
        interestedPeers.remove(peer);
        chokedInterestedPeers.remove(peer);
        unchokedPeers.remove(peer);
        activePeers.remove(peer);

        uploaders.remove(peer);

        unchokedNotInterestedPeers.remove(peer);
        if (plannedOptimisticUnchokedPeer != null
                && plannedOptimisticUnchokedPeer.equals(peer)) {
            plannedOptimisticUnchokedPeer = null;
        }
        PeerInfo info = peers.remove(peer);

        // discard all pieces in transit coming from this neighbor
        LinkedList<PieceInTransit> piecesFromPeer = new LinkedList<PieceInTransit>(
                partialPieces.values());
        for (PieceInTransit piece : piecesFromPeer) {
            if (piece.fromPeer.equals(peer)) {
                partialPieces.remove(piece);
                piecesInTransit.reset(piece.pieceIndex);
            }
        }

        // adjust local piece availability
        pieceAvailability.removePeer(info.pieces);

        // re-choke
        chokeUnchoke(false);

        // contact tracker for more peers if now we have < 20
        if (peers.size() < configuration.getMinPeersThreshold()) {
            TrackerRequestMessage request = new TrackerRequestMessage(self,
                    torrent.getTracker(), torrent.getTorrentId(), "0", "0",
                    "left", configuration.getMaxInitiatedConnections(), null);
            trigger(request, network);
        }
    }

    // ============= DEBUG =============
    private void logDebugState() {
        logger.debug(
                "C={}, UN={}, I={}, CI={}, A={}, UP={}, PP={}, H={}, BF={}",
                new Object[]{chokedPeers, unchokedPeers, interestedPeers,
                    chokedInterestedPeers, activePeers, uploaders,
                    partialPieces.keySet(), myPieces.cardinality(),
                    myPieces});
    }
}
