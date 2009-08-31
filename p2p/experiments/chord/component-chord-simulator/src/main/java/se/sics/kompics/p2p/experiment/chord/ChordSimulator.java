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
package se.sics.kompics.p2p.experiment.chord;

import java.math.BigInteger;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ChannelFilter;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;
import se.sics.kompics.p2p.fd.ping.PingFailureDetectorConfiguration;
import se.sics.kompics.p2p.monitor.chord.server.ChordMonitorConfiguration;
import se.sics.kompics.p2p.overlay.chord.ChordConfiguration;
import se.sics.kompics.p2p.overlay.chord.ChordLookupRequest;
import se.sics.kompics.p2p.overlay.chord.ChordLookupResponse;
import se.sics.kompics.p2p.overlay.chord.ChordNeighborsResponse;
import se.sics.kompics.p2p.overlay.chord.ChordLookupResponse.ChordLookupStatus;
import se.sics.kompics.p2p.overlay.chord.router.FindSuccessorRequest;
import se.sics.kompics.p2p.overlay.key.NumericRingKey;
import se.sics.kompics.p2p.peer.chord.ChordPeer;
import se.sics.kompics.p2p.peer.chord.ChordPeerInit;
import se.sics.kompics.p2p.peer.chord.ChordPeerPort;
import se.sics.kompics.p2p.peer.chord.JoinChordRing;
import se.sics.kompics.p2p.peer.chord.LeaveChordRing;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.web.Web;
import se.sics.kompics.web.WebRequest;

/**
 * The <code>ChordSimulator</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class ChordSimulator extends ComponentDefinition {

	Positive<ChordExperiment> simulator = positive(ChordExperiment.class);
	Positive<Network> network = positive(Network.class);
	Positive<Timer> timer = positive(Timer.class);
	Negative<Web> web = negative(Web.class);

	private static final Logger logger = LoggerFactory
			.getLogger(ChordSimulator.class);
	private final HashMap<NumericRingKey, Component> peers;

	// peer initialization state
	private Address peer0Address;
	private BootstrapConfiguration bootstrapConfiguration;
	private ChordMonitorConfiguration monitorConfiguration;
	private ChordConfiguration chordConfiguration;
	private PingFailureDetectorConfiguration fdConfiguration;

	private int peerIdSequence;

	private BigInteger chordRingSize;

	private ConsistentHashtable<NumericRingKey> chordRingView;

	// chord statistics
	private long totalPeerLifetime = 0;
	private long currentPeriodStartedAt = 0;
	private int currentPeriodPeerCount = 0;
	private ChordDataSet dataSet;

	public ChordSimulator() {
		peers = new HashMap<NumericRingKey, Component>();
		chordRingView = new ConsistentHashtable<NumericRingKey>();

		subscribe(handleInit, control);

		subscribe(handleJoin, simulator);
		subscribe(handleLeave, simulator);
		subscribe(handleFail, simulator);
		subscribe(handleLookup, simulator);
		subscribe(handleCollectData, simulator);
		subscribe(handleMessage, network);
	}

	Handler<ChordSimulatorInit> handleInit = new Handler<ChordSimulatorInit>() {
		public void handle(ChordSimulatorInit init) {
			peers.clear();
			peerIdSequence = 0;
			dataSet = null;

			peer0Address = init.getPeer0Address();
			bootstrapConfiguration = init.getBootstrapConfiguration();
			monitorConfiguration = init.getMonitorConfiguration();
			chordConfiguration = init.getChordConfiguration();
			fdConfiguration = init.getFdConfiguration();

			chordRingSize = new BigInteger("2").pow(chordConfiguration
					.getLog2RingSize());
		}
	};

	Handler<ChordPeerJoin> handleJoin = new Handler<ChordPeerJoin>() {
		public void handle(ChordPeerJoin event) {
			NumericRingKey key = event.getNodeKey();

			// join with the next id if this id is taken
			NumericRingKey successor = chordRingView.getNode(key);
			while (successor != null && successor.equals(key)) {
				key = key.successor(chordRingSize);
				successor = chordRingView.getNode(key);
			}

			logger.debug("JOIN@{}", key);

			Component newPeer = createAndStartNewPeer(key);

			chordRingView.addNode(key);

			trigger(new JoinChordRing(key), newPeer
					.getPositive(ChordPeerPort.class));

			addedPeer();
		}
	};

	Handler<ChordPeerLeave> handleLeave = new Handler<ChordPeerLeave>() {
		public void handle(ChordPeerLeave event) {
			if (chordRingView.size() == 0) {
				System.err.println("Empty network");
				return;
			}

			NumericRingKey key = chordRingView.getNode(event.getNodeKey());

			logger.debug("LEAVE@" + key);

			Component peer = peers.get(key);
			trigger(new LeaveChordRing(), peer.getPositive(ChordPeerPort.class));

			chordRingView.removeNode(key);
			removedPeer();

			stopAndDestroyPeer(key);
		}
	};

	Handler<ChordPeerFail> handleFail = new Handler<ChordPeerFail>() {
		public void handle(ChordPeerFail event) {
			if (chordRingView.size() == 0) {
				System.err.println("Empty network");
				return;
			}

			NumericRingKey key = chordRingView.getNode(event.getNodeKey());

			logger.debug("FAIL@" + key);

			chordRingView.removeNode(key);
			removedPeer();

			stopAndDestroyPeer(key);
		}
	};

	Handler<ChordLookup> handleLookup = new Handler<ChordLookup>() {
		private int lcnt = 0;

		public void handle(ChordLookup event) {
			NumericRingKey node = chordRingView.getNode(event.getNodeKey());
			NumericRingKey key = event.getLookupKey();

			logger.debug("{} LOOKUP@{} for {}", new Object[] { ++lcnt, node,
					key });

			ChordLookupRequest lookupRequest = new ChordLookupRequest(key, node);
			trigger(lookupRequest, peers.get(node).getPositive(
					ChordPeerPort.class));
		}
	};

	Handler<ChordLookupResponse> handleLookupResponse = new Handler<ChordLookupResponse>() {
		public void handle(ChordLookupResponse event) {
			// logger
			// .info(
			// "LOOKUP_RESP@{} for {} is {} but I think {}",
			// new Object[] {
			// event.getAttachment(),
			// event.getKey(),
			// (event.getStatus() == ChordLookupStatus.SUCCESS ? event
			// .getResponsible()
			// : "FAILED"),
			// chordRingView.getNode(event.getKey()) });

			if (dataSet == null) {
				return;
			}
			boolean success = event.getStatus().equals(
					ChordLookupStatus.SUCCESS);
			boolean correct = event.getResponsible() != null ? chordRingView
					.getNode(event.getKey()).equals(
							event.getResponsible().getKey()) : false;

			int hopCount = 0;
			long latency = 0;
			if (success) {
				hopCount = event.getLookupInfo().getHopCount();
				latency = event.getLookupInfo().getDuration();
			}

			ChordLookupStat stat = new ChordLookupStat(success, correct,
					(NumericRingKey) event.getAttachment(), event.getKey(),
					event.getResponsible().getKey(), latency, hopCount);

			dataSet.lookups.add(stat);
			dataSet.totalLookups++;
			if (success) {
				dataSet.successLookups++;
				if (correct) {
					dataSet.correctLookups++;
				}
			} else {
				dataSet.failedLookups++;
			}
		}
	};

	private HashMap<Class<? extends Message>, ReceivedMessage> messageHistogram = new HashMap<Class<? extends Message>, ReceivedMessage>();
	private HashMap<NumericRingKey, Integer> loadHistogram = new HashMap<NumericRingKey, Integer>();

	Handler<Message> handleMessage = new Handler<Message>() {
		public void handle(Message event) {
			ReceivedMessage rm = messageHistogram.get(event.getClass());
			if (rm == null) {
				rm = new ReceivedMessage(event.getClass(), 0);
				messageHistogram.put(event.getClass(), rm);
			}
			rm.incrementCount();

			if (dataSet != null) {
				rm = dataSet.messageHistogram.get(event.getClass());
				if (rm == null) {
					rm = new ReceivedMessage(event.getClass(), 0);
					dataSet.messageHistogram.put(event.getClass(), rm);
				}
				rm.incrementCount();
			}

			// lookup load
			if (event instanceof FindSuccessorRequest) {
				FindSuccessorRequest req = (FindSuccessorRequest) event;
				if (!req.isMaintenance()) {
					NumericRingKey node = req.getChordDestination().getKey();
					Integer count = loadHistogram.get(node);
					if (count == null) {
						loadHistogram.put(node, 1);
					} else {
						loadHistogram.put(node, count + 1);
					}
					if (dataSet != null) {
						count = dataSet.loadHistogram.get(node);
						if (count == null) {
							dataSet.loadHistogram.put(node, 1);
						} else {
							dataSet.loadHistogram.put(node, count + 1);
						}
					}
				}
			}
		}
	};

	// private int snapshotPeerCount;

	Handler<CollectData> handleCollectData = new Handler<CollectData>() {
		public void handle(CollectData event) {
			if (dataSet == null) {
				dataSet = new ChordDataSet();
				dataSet.beganAt = System.currentTimeMillis();
				logger.info("Started data collection...");
			} else {
				dataSet.endedAt = System.currentTimeMillis();
				logger.info("Stopped data collection...");

				updatedPeerCount(0);
				ChordDataPoint dataPoint = new ChordDataPoint(
						currentPeriodPeerCount, dataSet);
				logger.info("DataPoint: \n{}", dataPoint);
			}

			// updatedPeerCount(0);

			// snapshotPeerCount = 0;
			// snapshotPeers = new TreeMap<NumericRingKey,
			// ChordNeighborsResponse>();

			// LinkedList<ReceivedMessage> receivedMessages = new
			// LinkedList<ReceivedMessage>(
			// messageHistogram.values());
			// Collections.sort(receivedMessages);
			//
			// logger.info("Received Messages");
			// logger.info("-----------------");
			// for (ReceivedMessage receivedMessage : receivedMessages) {
			// logger.info(receivedMessage.toString());
			// }
			// logger.info("-----------------");

			// for (Class<? extends Message> messageType : messageHistogram
			// .keySet()) {
			// logger.info("{} = {}", messageType, messageHistogram
			// .get(messageType));
			// }

			// for (Component peer : peers.values()) {
			// trigger(new ChordNeighborsRequest(), peer
			// .getPositive(ChordPeerPort.class));
			// snapshotPeerCount++;
			// }
		}
	};

	// TreeMap<NumericRingKey, ChordNeighborsResponse> snapshotPeers;

	Handler<ChordNeighborsResponse> handleNeighbors = new Handler<ChordNeighborsResponse>() {
		public void handle(ChordNeighborsResponse event) {
			// ChordAddress node = event.getNeighbors().getLocalPeer();
			// snapshotPeerCount--;
			// snapshotPeers.put(node.getKey(), event);
			//
			// if (snapshotPeerCount == 0) {
			// // dump view
			// for (NumericRingKey key : snapshotPeers.keySet()) {
			// ChordNeighborsResponse r = snapshotPeers.get(key);
			// logger.info("Node={} P={} S={}", new Object[] { key,
			// r.getNeighbors().getPredecessorPeer(),
			// r.getNeighbors().getSuccessorPeer() });
			// }
			// }
		}
	};

	private final void addedPeer() {
		updatedPeerCount(1);
	}

	private final void removedPeer() {
		updatedPeerCount(-1);
	}

	private final void updatedPeerCount(int increment) {
		long now = System.currentTimeMillis();
		long period = now - currentPeriodStartedAt;
		totalPeerLifetime += period * currentPeriodPeerCount;
		currentPeriodStartedAt = now;
		currentPeriodPeerCount += increment;

		if (totalPeerLifetime < 0)
			throw new RuntimeException("Total peer lifetime overflow");

		if (increment == 0) {
			logger
					.info("Total peer lifetime: {} Current peer count: {}",
							durationToString(totalPeerLifetime),
							currentPeriodPeerCount);
		} else {
			logger.debug("Period: {} Total lifetime: {} Current count: {}",
					new Object[] { durationToString(period),
							durationToString(totalPeerLifetime),
							currentPeriodPeerCount });
		}
	}

	/**
	 * The <code>MessageDestinationFilter</code> class.
	 * 
	 * @author Cosmin Arad <cosmin@sics.se>
	 * @version $Id: MessageDestinationFilter.java 750 2009-04-02 09:55:01Z
	 *          Cosmin $
	 */
	private final static class MessageDestinationFilter extends
			ChannelFilter<Message, Address> {
		public MessageDestinationFilter(Address address) {
			super(Message.class, address, true);
		}

		public Address getValue(Message event) {
			return event.getDestination();
		}
	}

	/**
	 * The <code>WebRequestDestinationFilter</code> class.
	 * 
	 * @author Cosmin Arad <cosmin@sics.se>
	 * @version $Id: WebRequestDestinationFilter.java 750 2009-04-02 09:55:01Z
	 *          Cosmin $
	 */
	private final static class WebRequestDestinationFilter extends
			ChannelFilter<WebRequest, Integer> {
		public WebRequestDestinationFilter(Integer destination) {
			super(WebRequest.class, destination, false);
		}

		public Integer getValue(WebRequest event) {
			return event.getDestination();
		}
	}

	private final Component createAndStartNewPeer(NumericRingKey nodeKey) {
		Component peer = create(ChordPeer.class);
		int peerId = ++peerIdSequence;
		Address peerAddress = new Address(peer0Address.getIp(), peer0Address
				.getPort(), peerId);

		connect(network, peer.getNegative(Network.class),
				new MessageDestinationFilter(peerAddress));
		connect(timer, peer.getNegative(Timer.class));
		connect(web, peer.getPositive(Web.class),
				new WebRequestDestinationFilter(peerId));

		subscribe(handleLookupResponse, peer.getPositive(ChordPeerPort.class));
		subscribe(handleNeighbors, peer.getPositive(ChordPeerPort.class));

		trigger(new ChordPeerInit(peerAddress, bootstrapConfiguration,
				monitorConfiguration, chordConfiguration, fdConfiguration),
				peer.getControl());

		trigger(new Start(), peer.getControl());
		peers.put(nodeKey, peer);

		return peer;
	}

	private final void stopAndDestroyPeer(NumericRingKey nodeKey) {
		Component peer = peers.get(nodeKey);

		trigger(new Stop(), peer.getControl());

		unsubscribe(handleLookupResponse, peer.getPositive(ChordPeerPort.class));
		unsubscribe(handleNeighbors, peer.getPositive(ChordPeerPort.class));

		disconnect(network, peer.getNegative(Network.class));
		disconnect(timer, peer.getNegative(Timer.class));
		disconnect(web, peer.getPositive(Web.class));

		peers.remove(nodeKey);
		destroy(peer);
	}

	private static final String durationToString(long duration) {
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
