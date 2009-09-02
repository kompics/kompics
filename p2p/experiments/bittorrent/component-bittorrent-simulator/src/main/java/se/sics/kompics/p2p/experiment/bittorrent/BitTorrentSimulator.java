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
package se.sics.kompics.p2p.experiment.bittorrent;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.cdn.bittorrent.BitTorrentConfiguration;
import se.sics.kompics.p2p.cdn.bittorrent.TorrentMetadata;
import se.sics.kompics.p2p.cdn.bittorrent.address.BitTorrentAddress;
import se.sics.kompics.p2p.cdn.bittorrent.client.BitTorrentClient;
import se.sics.kompics.p2p.cdn.bittorrent.client.BitTorrentClientInit;
import se.sics.kompics.p2p.cdn.bittorrent.client.BitTorrentClientPort;
import se.sics.kompics.p2p.cdn.bittorrent.client.Bitfield;
import se.sics.kompics.p2p.cdn.bittorrent.client.DownloadCompleted;
import se.sics.kompics.p2p.cdn.bittorrent.client.JoinSwarm;
import se.sics.kompics.p2p.cdn.bittorrent.message.BitTorrentMessage;
import se.sics.kompics.p2p.experiment.bittorrent.bw.model.BwDelayedMessage;
import se.sics.kompics.p2p.experiment.bittorrent.bw.model.Link;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

/**
 * The <code>BitTorrentSimulator</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class BitTorrentSimulator extends ComponentDefinition {

	Positive<BitTorrentSimulatorPort> simulator = positive(BitTorrentSimulatorPort.class);
	Positive<Network> network = positive(Network.class);
	Positive<Timer> timer = positive(Timer.class);

	private static final Logger logger = LoggerFactory
			.getLogger(BitTorrentSimulator.class);
	private final HashMap<BigInteger, Component> peers;
	private final HashMap<BigInteger, Link> uploadLink;
	private final HashMap<BigInteger, Link> downloadLink;

	// peer initialization state
	private BitTorrentAddress trackerAddress;
	private TorrentMetadata torrent;
	private BitTorrentConfiguration btConfiguration;

	private int peerIdSequence;
	private int initialSeeds = 0, seedsExpected = 0;

	private ConsistentHashtable<BigInteger> peersView;
	private boolean selfishPeers;

	// statistics
	private HashMap<BigInteger, Long> leecherDownloadTime;
	private HashMap<Class<? extends Message>, ReceivedMessage> messageHistogram;

	public BitTorrentSimulator() {
		peers = new HashMap<BigInteger, Component>();
		uploadLink = new HashMap<BigInteger, Link>();
		downloadLink = new HashMap<BigInteger, Link>();
		peersView = new ConsistentHashtable<BigInteger>();

		leecherDownloadTime = new HashMap<BigInteger, Long>();
		messageHistogram = new HashMap<Class<? extends Message>, ReceivedMessage>();

		subscribe(handleInit, control);

		subscribe(handleJoin, simulator);
		subscribe(handleMessageReceived, network);
		subscribe(handleDelayedMessage, timer);
	}

	Handler<BitTorrentSimulatorInit> handleInit = new Handler<BitTorrentSimulatorInit>() {
		public void handle(BitTorrentSimulatorInit init) {
			peers.clear();
			peerIdSequence = 0;

			btConfiguration = init.getBtConfiguration();
			torrent = init.getTorrent();
			trackerAddress = torrent.getTracker();
			selfishPeers = init.isSelfishPeers();
		}
	};

	Handler<BitTorrentPeerJoin> handleJoin = new Handler<BitTorrentPeerJoin>() {
		public void handle(BitTorrentPeerJoin event) {
			BigInteger id = event.getPeerId();

			// join with the next id if this id is taken
			BigInteger successor = peersView.getNode(id);
			while (successor != null && successor.equals(id)) {
				id = id.add(BigInteger.ONE);
				successor = peersView.getNode(id);
			}
			logger.debug("JOIN@{}", id);

			Component newPeer = createAndStartNewPeer(id,
					event.getDownloaded(), event.getDownloadBw(), event
							.getUploadBw());
			peersView.addNode(id);

			trigger(new JoinSwarm(), newPeer
					.getPositive(BitTorrentClientPort.class));

			if (event.getDownloaded().allSet()) {
				initialSeeds++;
			} else {
				long startedDownloadAt = System.currentTimeMillis();
				leecherDownloadTime.put(id, startedDownloadAt);
			}
			seedsExpected++;
		}
	};

	Handler<DownloadCompleted> handleCompleted = new Handler<DownloadCompleted>() {
		public void handle(DownloadCompleted event) {
			seedsExpected--;
			if (initialSeeds > 0) {
				// one of the initial seeds tells us it has all pieces
				initialSeeds--;
			} else {
				// a leecher just became a seed
				logger.debug("Peer {} completed download.", event.getPeer());
				BigInteger peerId = event.getPeer().getPeerId();

				long now = System.currentTimeMillis();
				long started = leecherDownloadTime.get(peerId);
				leecherDownloadTime.put(peerId, now - started);

				if (selfishPeers) {
					// if leecher is selfish we remove it from the swarm
					stopAndDestroyPeer(peerId);
				}

				if (seedsExpected == 0) {
					// all joined peers have completed the download so we can
					// terminate the simulation
					trigger(new TerminateExperiment(), simulator);

					logStatistics();
				}
			}
		}
	};

	private void logStatistics() {
		SummaryStatistics downloadTime = new SummaryStatistics();
		for (long time : leecherDownloadTime.values()) {
			downloadTime.addValue(time);
		}

		int messages = 0;
		long traffic = 0;
		for (ReceivedMessage rm : messageHistogram.values()) {
			messages += rm.getTotalCount();
			traffic += rm.getTotalSize();
		}

		long torrentSize = torrent.getPieceCount() * torrent.getPieceSize();

		logger.info("=================================================");
		logger.info("Content size: {} bytes", torrentSize);
		logger.info("Piece size:   {} bytes", torrent.getPieceSize());
		logger.info("Piece count:  {}", torrent.getPieceCount());
		logger.info("=================================================");
		logger.info("Number of leechers: {}", downloadTime.getN());
		logger.info("Min download time:  {} ms ({})", downloadTime.getMin(),
				durationToString(Math.round(downloadTime.getMin())));
		logger.info("Max download time:  {} ms ({})", downloadTime.getMax(),
				durationToString(Math.round(downloadTime.getMax())));
		logger.info("Avg download time:  {} ms ({})", downloadTime.getMean(),
				durationToString(Math.round(downloadTime.getMean())));
		logger.info("Std download time:  {} ms ({})", downloadTime
				.getStandardDeviation(), durationToString(Math
				.round(downloadTime.getStandardDeviation())));
		logger.info("Min download rate:  {} Bps", torrentSize
				/ downloadTime.getMax() * 1000);
		logger.info("Max download rate:  {} Bps", torrentSize
				/ downloadTime.getMin() * 1000);
		logger.info("Avg download rate:  {} Bps", torrentSize
				/ downloadTime.getMean() * 1000);
		logger.info("=================================================");
		logger.info("Total number of messages: {}", messages);
		logger.info("Total amount of traffic:  {} bytes", traffic);
		for (Map.Entry<Class<? extends Message>, ReceivedMessage> entry : messageHistogram
				.entrySet()) {
			logger.info("{}: #={}  \t bytes={}", new Object[] {
					String.format("%22s", entry.getKey().getSimpleName()),
					entry.getValue().getTotalCount(),
					entry.getValue().getTotalSize() });
		}
		logger.info("=================================================");
	}

	Handler<BitTorrentMessage> handleMessageSent = new Handler<BitTorrentMessage>() {
		public void handle(BitTorrentMessage message) {
			// message just sent by some peer goes into peer's up pipe
			Link link = uploadLink.get(message.getBitTorrentSource()
					.getPeerId());
			long delay = link.addMessage(message);
			if (delay == 0) {
				// immediately send to cloud
				trigger(message, network);
				return;
			}
			ScheduleTimeout st = new ScheduleTimeout(delay);
			st.setTimeoutEvent(new BwDelayedMessage(st, message, true));
			trigger(st, timer);
		}
	};

	Handler<BitTorrentMessage> handleMessageReceived = new Handler<BitTorrentMessage>() {
		public void handle(BitTorrentMessage message) {
			// traffic stats
			ReceivedMessage rm = messageHistogram.get(message.getClass());
			if (rm == null) {
				rm = new ReceivedMessage(message.getClass(), 0, 0);
				messageHistogram.put(message.getClass(), rm);
			}
			rm.incrementCount();
			rm.incrementSize(message.getSize());

			// message to be received by some peer goes into peer's down pipe
			Link link = downloadLink.get(message.getBitTorrentDestination()
					.getPeerId());
			if (link == null)
				return;
			long delay = link.addMessage(message);
			if (delay == 0) {
				// immediately deliver to peer
				Component peer = peers.get(message.getBitTorrentDestination()
						.getPeerId());
				trigger(message, peer.getNegative(Network.class));
				return;
			}
			ScheduleTimeout st = new ScheduleTimeout(delay);
			st.setTimeoutEvent(new BwDelayedMessage(st, message, false));
			trigger(st, timer);
		}
	};

	Handler<BwDelayedMessage> handleDelayedMessage = new Handler<BwDelayedMessage>() {
		public void handle(BwDelayedMessage delayedMessage) {
			if (delayedMessage.isBeingSent()) {
				// message comes out of upload pipe
				BitTorrentMessage message = delayedMessage.getMessage();
				// and goes to the network cloud
				trigger(message, network);
			} else {
				// message comes out of download pipe
				BitTorrentMessage message = delayedMessage.getMessage();
				Component peer = peers.get(message.getBitTorrentDestination()
						.getPeerId());
				if (peer != null) {
					// and goes to the peer
					trigger(message, peer.getNegative(Network.class));
				}
			}
		}
	};

	private final Component createAndStartNewPeer(BigInteger id,
			Bitfield initialPieces, long downloadBw, long uploadBw) {
		Component peer = create(BitTorrentClient.class);
		int peerId = ++peerIdSequence;
		Address peerAddress = new Address(trackerAddress.getPeerAddress()
				.getIp(), trackerAddress.getPeerAddress().getPort(), peerId);

		connect(timer, peer.getNegative(Timer.class));

		subscribe(handleCompleted, peer.getPositive(BitTorrentClientPort.class));
		subscribe(handleMessageSent, peer.getNegative(Network.class));

		trigger(new BitTorrentClientInit(
				new BitTorrentAddress(peerAddress, id), initialPieces, torrent,
				btConfiguration), peer.getControl());

		trigger(new Start(), peer.getControl());
		peers.put(id, peer);
		uploadLink.put(id, new Link(uploadBw));
		downloadLink.put(id, new Link(downloadBw));

		return peer;
	}

	private final void stopAndDestroyPeer(BigInteger id) {
		Component peer = peers.get(id);

		trigger(new Stop(), peer.getControl());

		unsubscribe(handleCompleted, peer
				.getPositive(BitTorrentClientPort.class));
		unsubscribe(handleMessageSent, peer.getNegative(Network.class));

		disconnect(timer, peer.getNegative(Timer.class));

		peers.remove(id);
		uploadLink.remove(id);
		downloadLink.remove(id);
		destroy(peer);
	}

	public static final String durationToString(long duration) {
		StringBuilder sb = new StringBuilder();
		int ms = 0, s = 0, m = 0, h = 0, d = 0, y = 0;

		ms = (int) (duration % 1000);
		// get duration in seconds
		duration /= 1000;
		s = (int) (duration % 60);
		// get duration in minutes
		duration /= 60;
		if (duration > 0) {
			m = (int) (duration % 60);
			// get duration in hours
			duration /= 60;
			if (duration > 0) {
				h = (int) (duration % 24);
				// get duration in days
				duration /= 24;
				if (duration > 0) {
					d = (int) (duration % 365);
					// get duration in years
					y = (int) (duration / 365);
				}
			}
		}
		boolean printed = false;
		if (y > 0) {
			sb.append(y).append("y ");
			printed = true;
		}
		if (d > 0) {
			sb.append(d).append("d ");
			printed = true;
		}
		if (h > 0) {
			sb.append(h).append("h ");
			printed = true;
		}
		if (m > 0) {
			sb.append(m).append("m ");
			printed = true;
		}
		if (s > 0 || !printed) {
			sb.append(s);
			if (ms > 0) {
				sb.append(".").append(String.format("%03d", ms));
			}
			sb.append("s");
		}
		return sb.toString();
	}
}
